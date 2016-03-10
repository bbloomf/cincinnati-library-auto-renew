package myapp;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.servlet.http.HttpServletResponse;

import com.gargoylesoftware.htmlunit.FailingHttpStatusCodeException;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.WebRequest;
import com.gargoylesoftware.htmlunit.html.DomElement;
import com.gargoylesoftware.htmlunit.html.DomNodeList;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

public class LibraryRenewer {
	private static Pattern ptnDueDate = Pattern.compile("(?:due\\s*\\d+-\\d+-\\d+\\s*renewed\\s*)?(?:now )?due ((\\d+)-(\\d+)-(\\d+))(\\s+.*)?", Pattern.CASE_INSENSITIVE);
	private static class Status {
		public String statusText;
		public Date nextDueDate;
		public int failCount;
		public int tryToRenewCount;
		
		public Status(String text, Date date, int failCount, int tryToRenewCount) {
			this.statusText = text;
			this.nextDueDate = date;
			this.failCount = failCount;
			this.tryToRenewCount = tryToRenewCount;
		}
	}
	
	private static Date getDateForCellText(String text) {
		Matcher m = ptnDueDate.matcher(text);
		if (m.matches()) {
			try {
				return Util.libraryDateFormat.parse(m.group(1));
			} catch (ParseException e) {
				e.printStackTrace();
			}
		}
		return null;
	}
	
	public static void email(LibraryCard card, String subject, String body) {
		Config cfg = Config.load();
		String masterEmail = null;
		if (cfg != null)
			masterEmail = cfg.master_email;
		
		String from = Util.getFromEmail();
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from, "Cincinnati Library Auto Renew"));
			String userEmail = card==null? masterEmail : card.user.get().email;
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(userEmail));
			if(card != null && !userEmail.equalsIgnoreCase(card.email)) {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(card.email));
			}
			msg.setSubject(subject);
			msg.setText(body);
			Transport.send(msg);
		} catch (AddressException e) {
			System.out.println("Failed to send email; stack trace follows.");
			e.printStackTrace(System.out);
		} catch (MessagingException e) {
			System.out.println("Failed to send email; stack trace follows.");
			e.printStackTrace(System.out);
		} catch (UnsupportedEncodingException e) {
			System.out.println("Failed to send email; stack trace follows.");
			e.printStackTrace(System.out);
		}
	}
	
	private static List<AvailableItemStatus> statusesInTable(HtmlTable table) {
		ArrayList<AvailableItemStatus> result = new ArrayList<AvailableItemStatus>();
		int statusCol = -1;
	    int rowId = 0;
	    for(final HtmlTableRow row : table.getRows()) {
	    	int colId = 0;
	    	for(final HtmlTableCell cell : row.getCells()) {
  			if(rowId == 0) {
	    			if(cell.asText().trim().equalsIgnoreCase("status")) {
	    				statusCol = colId;
	    				break;
	    			}
	    		} else {
		    		if(colId == statusCol) {
		    			result.add(AvailableItemStatus.findOrCreate(cell.asText().trim()));
		    		}
	    		}
	    		++colId;
	    	}
	    	++rowId;
	    }
	    return result;
	}
	
	public static int itemStatus(String url) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return itemStatus(url, null);
	}
	
	// Returns integer correpsonding to whether the item is likely to be able to be renewed
	// 0: not likely
	// 1: likely
	public static int itemStatus(String url, Integer expectedResult) throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
		final WebClient webClient = new WebClient();
		try {
			int result = 1;
			boolean hasHolds = false;
			StringBuilder sb = new StringBuilder(String.format("url: %s\n", url));
		    webClient.getOptions().setThrowExceptionOnScriptError(false);
			webClient.getOptions().setPrintContentOnFailingStatusCode(false);
			HtmlPage page = webClient.getPage(url);
			HtmlElement document = page.getDocumentElement();
			DomElement titleElem = page.getElementById("bibTitle");
			if(titleElem != null) {
				sb.append(String.format("title: %s\n", titleElem.asText().trim()));
			}
			List<HtmlElement> dpBibHoldingStatement = document.getElementsByAttribute("div", "class", "dpBibHoldingStatement");
			List<HtmlElement> holdsMessage = document.getElementsByAttribute("div", "class", "holdsMessage");
		    List<HtmlElement> itemsAvailable = document.getElementsByAttribute("span", "class", "itemsAvailable");
		    List<HtmlElement> itemsNotAvailable = document.getElementsByAttribute("span", "class", "itemsNotAvailable");
		    List<HtmlElement> allItemsTable = document.getElementsByAttribute("div", "class", "allItemsSection");
		    if(!allItemsTable.isEmpty()) {
		    	allItemsTable = allItemsTable.get(0).getElementsByAttribute("table", "class", "itemTable");
		    }
		    List<HtmlElement> availableItemsTable = document.getElementsByAttribute("div", "class", "availableItemsSection");
		    if(!availableItemsTable.isEmpty()) {
		    	availableItemsTable = availableItemsTable.get(0).getElementsByAttribute("table", "class", "itemTable");
		    }
		    if(!dpBibHoldingStatement.isEmpty()) {
		      sb.append(String.format("dpBibHoldingStatement: %s\n", dpBibHoldingStatement.get(0).asText()));
		    }
		    if(!holdsMessage.isEmpty()) {
		      hasHolds = true;
		      sb.append(String.format("holdsMessage: %s\n", holdsMessage.get(0).asText()));
		    }
		    if(!itemsAvailable.isEmpty()) {
		      sb.append(String.format("itemsAvailable: %s\n", itemsAvailable.get(0).asText()));
		    }
		    if(!itemsNotAvailable.isEmpty()) {
		      sb.append(String.format("itemsNotAvailable: %s\n", itemsNotAvailable.get(0).asText()));
		    }
		    if(itemsAvailable.isEmpty() && itemsNotAvailable.isEmpty()) {
		    	// unknown state...has the page changed?
		    	email(null, "Problem with item status", String.format("This url %s contained neither span.itemsAvailable nor span.itemsNotAvailable", url));
		    }
		    List<AvailableItemStatus> availableStatuses = null;
		    if(!availableItemsTable.isEmpty()) {
		        availableStatuses = statusesInTable((HtmlTable)availableItemsTable.get(0));
		        boolean canBePutOnHold = false;
		        for(AvailableItemStatus s : availableStatuses) {
		        	if(s.canBePutOnHold) {
		        		canBePutOnHold = true;
		        		break;
		        	}
		        }
		        result = canBePutOnHold? 1 : (hasHolds? 0 : 1);
		    } else if(!itemsNotAvailable.isEmpty()) {
		    	result = hasHolds? 0 : 1;
		    }
		    System.out.println(sb.toString());
		    if(expectedResult != null && expectedResult != result) {
		    	if(result == 1) {
		    		email(null, "Item failed to renew, but according to itemStatus() it should have succeeded.", page.asXml());
		    	}
		    }
		    return result;
		} finally {
			webClient.close();
		}
	}

	public static Status processStatusPage(HtmlPage page, LibraryCard card, boolean isTask, int triedToRenew) {
		String status = null;
		User user = card.user.get();
		int failedCount = 0;
		int tryToRenewCount = 0;
		HtmlElement ele = page.getHtmlElementById("renewfailmsg");
		Date nextDueDate = null;
		if (ele != null && !ele.getElementsByTagName("h2").isEmpty()) {
			StringBuilder sb = new StringBuilder(256);
			sb.append(ele.getElementsByTagName("h2").get(0).asText()).append("\n\n");
			HtmlTable table = (HtmlTable) page.getElementsByTagName("table").get(0);
			int rowId = 0;
			HashMap<Integer, String> column = new HashMap<Integer, String>();
			for (final HtmlTableRow row : table.getRows()) {
				int colId = 0;
				HashMap<String, HtmlTableCell> workingRow = null;
				if (rowId > 1) {
					workingRow = new HashMap<String, HtmlTableCell>();
				}
				for (final HtmlTableCell cell : row.getCells()) {
					if (rowId == 1) {
						column.put(colId, cell.asText().toLowerCase());
					} else if (rowId > 1) {
						workingRow.put(column.get(colId), cell);
						if (column.get(colId).equals("status")) {
							List<HtmlElement> list = cell.getHtmlElementsByTagName("em");
							if (!list.isEmpty()) {
								ItemStatus itemStatus = ItemStatus.findOrCreate(list.get(0).asText(), page);
								++failedCount;
								if(itemStatus.worthTryingToRenew) {
									++tryToRenewCount;
									//make sure this is a failure that could be detected in the future:
									DomNodeList<HtmlElement> titleAnchors = workingRow.get("title").getElementsByTagName("a");
									if(titleAnchors.getLength() > 0) {
										try {
											itemStatus(titleAnchors.get(0).getAttribute("href"),isTask? null : 0);
										} catch (FailingHttpStatusCodeException e) {
										} catch (MalformedURLException e) {
										} catch (IOException e) {
										}
									}
								}
								HtmlTableCell titleCell = workingRow.get("title");
								String title = titleCell.asText().trim();
								DomNodeList<HtmlElement> nodes = titleCell.getElementsByTagName("a");
								if(nodes.getLength() > 0) {
									title = String.format("%s: %s", title, nodes.get(0).getAttribute("href"));
								}
								sb.append(itemStatus.text).append(": ").append(title).append("\n\n");
							}
							Date date = getDateForCellText(cell.asText());
							if (date != null && (nextDueDate == null || nextDueDate.after(date)))
								nextDueDate = date;
						}
					}
					++colId;
				}
				++rowId;
			}
			if (!isTask) {
				if(failedCount > 0) {
					status = String.format("%s of %s item%s failed to renew", failedCount, triedToRenew,
							triedToRenew == 1 ? "" : "s");
					if(tryToRenewCount > 0) {
						sb.append(String.format(
								"It will continue attempting to renew %s every 15 minutes.  Another email will be sent if %s is successfully renewed.\n",
								tryToRenewCount == 1 ? "this item" : "these items", tryToRenewCount == 1 ? "it" : "one"));
						Util.enqueueTask(user.email, card.card_number);
					}
					email(card, status, sb.toString());
				} else {
					status = String.format("Successfully renewed %s item%s\n", triedToRenew,
							triedToRenew == 1 ? "" : "s");
				}
			} else {
				if (failedCount != triedToRenew) {
					int successes = triedToRenew - failedCount;
					status = String.format("%s item%s succeeded in renewing, %s failed", successes,
							successes == 1 ? "" : "s", failedCount);
					email(card, status, sb.toString());
				} else {
					status = String.format("%s of %s item%s failed to renew", failedCount, triedToRenew,
							triedToRenew == 1 ? "" : "s");
				}
			}
			if(tryToRenewCount > 0) {
				status = status.concat(", retrying in 15 minutes");
			}
		}
		return new Status(status, nextDueDate, failedCount, tryToRenewCount);
	}

	public static int renew(LibraryCard card)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return renew(card, false, null, null);
	}

	public static int renewTask(LibraryCard card, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return renew(card, true, null, resp);
	}

	public static int renewTask(LibraryCard card, Date deadline, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return renew(card, true, deadline, resp);
	}

	public static int renew(LibraryCard card, boolean isTask, Date deadline, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		if(deadline == null) {
			deadline = card.user.get().vacationEnds();
		}
		Calendar cal = Calendar.getInstance(); 
		cal.set(Calendar.HOUR_OF_DAY, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		cal.add(Calendar.DAY_OF_MONTH, 7);
		Date nextWeek = cal.getTime();
		System.out.printf("Renewing items due on or before %s\n", Util.jsTime.format(deadline));
		Status renewalStatus = null;
		java.util.logging.Logger.getLogger("com.gargoylesoftware.htmlunit").setLevel(java.util.logging.Level.OFF);
		final WebClient webClient = new WebClient();
		webClient.getOptions().setThrowExceptionOnScriptError(false);
		webClient.getOptions().setPrintContentOnFailingStatusCode(false);
		HtmlPage page = webClient.getPage("https://classic.cincinnatilibrary.org:443/dp/patroninfo*eng/1180542/items");
		System.out.println(page.getUrl().toString());
		page.getHtmlElementById("code").type(card.card_number);
		HtmlElement pin = page.getHtmlElementById("pin");
		pin.type(card.pin);
		Date nextDueDate = null;

		WebRequest request = pin.getEnclosingForm().getWebRequest(null);
		request.setUrl(new URL(
				"https://classic.cincinnatilibrary.org/iii/cas/login?service=https%3A%2F%2Fcatalog.cincinnatilibrary.org%3A443%2Fiii%2Fencore%2Fj_acegi_cas_security_check"));
		page = webClient.getPage(request);

		String status = "";
		if (page.getUrl().toString().contains("login")) {
			HtmlElement statusElement = page.getHtmlElementById("status");
			if (statusElement != null) {
				status = "Error: " + statusElement.asText();
				card.UpdateStatus(status, null);
				if (resp != null)
					resp.getWriter().println(status);
				webClient.close();
				return 0;
			}
		}

		HtmlTable table = (HtmlTable) page.getElementsByTagName("table").get(0);
		int rowId = 0;
		HashMap<Integer, String> column = new HashMap<Integer, String>();
		HashMap<String, Integer> columnId = new HashMap<String, Integer>();
		if(card.email.equalsIgnoreCase(Config.load().master_email)) {
			System.out.printf("Checking renewability of items due through %s\n", Util.simpleDate.format(nextWeek));
		}
		int needToRenew = 0;
		for (final HtmlTableRow row : table.getRows()) {
			int colId = 0;
			HashMap<String, HtmlTableCell> workingRow = null;
			if (rowId > 1) {
				workingRow = new HashMap<String, HtmlTableCell>();
			}
			for (final HtmlTableCell cell : row.getCells()) {
				if (rowId == 1) {
					column.put(colId, cell.asText().toLowerCase());
					columnId.put(cell.asText().toLowerCase(), colId);
				} else if (rowId > 1) {
					workingRow.put(column.get(colId), cell);
					if ("status".equalsIgnoreCase(column.get(colId))) {
						Matcher m = ptnDueDate.matcher(cell.asText());
						if (m.matches()) {
							Date date;
							try {
								date = Util.libraryDateFormat.parse(m.group(1));
								if (nextDueDate == null)
									nextDueDate = date;
							} catch (ParseException e) {
								e.printStackTrace();
								if (resp != null) {
									e.printStackTrace(resp.getWriter());
								}
								card.UpdateStatus(String.format("Error parsing date: %s", cell.asText()), null);
								webClient.close();
								return 0;
							}
							if (card.email.equalsIgnoreCase(Config.load().master_email)) {
								if(date.compareTo(nextWeek) <= 0) {
									DomNodeList<HtmlElement> titleAnchors = workingRow.get("title").getElementsByTagName("a");
									if(titleAnchors.getLength() > 0) {
										try {
											HtmlElement anchor = titleAnchors.get(0);
											String title = anchor.asText().trim();
											String href = anchor.getAttribute("href");
											if(itemStatus(href) == 0) {
												email(null,"Item may fail to renew",
														String.format("The item %s %s which is due on %s may fail to renew according to current heuristic models.", title, href, Util.simpleDate.format(date)));
											}
										} catch (Exception e) {
											System.out.printf("Failed to check item %s (due on %s) for renewability: %s",
													titleAnchors.get(0).asText().trim(), Util.simpleDate.format(date),
													e.toString());
										}
									} else {
										System.out.printf("Not checking item %s due on %s for renewability because it doesn't have a link", workingRow.get("title").asText().trim(), Util.simpleDate.format(date));
									}
								}
							}
							if (date.compareTo(deadline) <= 0) {
								HtmlCheckBoxInput cb = (HtmlCheckBoxInput) workingRow.get("renew")
										.getElementsByTagName("input").get(0);
								cb.setChecked(true);
								++needToRenew;
							}
						} else {
							System.out.println("error: ");
							System.out.println(cell.asText());
						}
					}
				}
				// System.out.println(" Found cell: " + cell.asText());
				++colId;
			}
			++rowId;
		}
		if (needToRenew > 0) {
			HtmlAnchor anchor = page.getAnchorByText("Renew Marked");
			if (anchor != null) {
				System.out.printf("--- Renewing %s item(s) ---", needToRenew);
				page = anchor.click();
				// page is now a confirmation page: "The following item(s) will
				// be renewed, would you like to proceed?"
				anchor = page.getAnchorByText("Yes");
				if (anchor != null) {
					System.out.println("--- Confirming ---");
					page = anchor.click();
					System.out.println(page.asXml());
					renewalStatus = processStatusPage(page, card, isTask, needToRenew);
					status = renewalStatus.statusText;
					nextDueDate = renewalStatus.nextDueDate;
				} else {
					status = "No 'Yes' anchor";
				}
			} else {
				status = "No 'Renew Marked' anchor";
			}
		} else {
			status = "Nothing to renew";
		}
		card.UpdateStatus(status, nextDueDate);
		System.out.println(status);

		// System.out.println(page.getUrl().toString());
		// final String pageAsXml = page.asXml();
		// System.out.println(pageAsXml);
		webClient.close();
		if (resp != null) {
			resp.getWriter().println(status);
			resp.getWriter().println();
		}
		return renewalStatus == null? 0 : renewalStatus.tryToRenewCount;
	}
}
