package myapp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
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
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlCheckBoxInput;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.gargoylesoftware.htmlunit.html.HtmlTable;
import com.gargoylesoftware.htmlunit.html.HtmlTableCell;
import com.gargoylesoftware.htmlunit.html.HtmlTableRow;

public class LibraryRenewer {
	private static Pattern ptnDueDate = Pattern.compile("(?:due\\s*\\d+-\\d+-\\d+\\s*renewed\\s*)?(?:now )?due ((\\d+)-(\\d+)-(\\d+))(\\s+.*)?", Pattern.CASE_INSENSITIVE);
	private static SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");
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
				return dateFormat.parse(m.group(1));
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
			msg.setFrom(new InternetAddress(from));
			String userEmail = card==null? masterEmail : card.user.get().email;
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(userEmail));
			if(card != null && !userEmail.equalsIgnoreCase(card.email)) {
				msg.addRecipient(Message.RecipientType.TO, new InternetAddress(card.email));
			}
			if (card != null && masterEmail != null && !masterEmail.equalsIgnoreCase(userEmail) && !masterEmail.equalsIgnoreCase(card.email)) {
				msg.addRecipient(Message.RecipientType.CC, new InternetAddress(masterEmail));
			}
			msg.setSubject(subject);
			msg.setText(body);
			Transport.send(msg);
		} catch (AddressException e) {
			System.out.println("Failed to send email; stack trace follows.");
			e.printStackTrace(System.err);
		} catch (MessagingException e) {
			System.out.println("Failed to send email; stack trace follows.");
			e.printStackTrace(System.err);
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
							List<HtmlElement> list = cell.getHtmlElementsByTagName("font");
							if (!list.isEmpty()) {
								ItemStatus itemStatus = ItemStatus.findOrCreate(list.get(0).asText(), page);
								++failedCount;
								if(itemStatus.worthTryingToRenew) {
									++tryToRenewCount;
								}
								String title = workingRow.get("title").asText().trim();
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
			} else if (failedCount != triedToRenew) {
				int successes = triedToRenew - failedCount;
				status = String.format("%s item%s succeeded in renewing, %s failed", successes,
						successes == 1 ? "" : "s", failedCount);
				email(card, status, sb.toString());
			}
		}
		return new Status(status, nextDueDate, failedCount, tryToRenewCount);
	}

	public static int renew(LibraryCard card)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return renew(card, false, null);
	}

	public static int renewTask(LibraryCard card, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		return renew(card, true, resp);
	}

	public static int renew(LibraryCard card, boolean isTask, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		Status renewalStatus = null;
		final WebClient webClient = new WebClient();
		webClient.getOptions().setThrowExceptionOnScriptError(false);
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
		Calendar today = Calendar.getInstance();
		today.set(Calendar.HOUR_OF_DAY, 0);
		today.set(Calendar.MINUTE, 0);
		today.set(Calendar.SECOND, 0);
		today.set(Calendar.MILLISECOND, 0);
		System.out.printf("Renewing items due on or before %s\n", dateFormat.format(today.getTime()));
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
								date = dateFormat.parse(m.group(1));
								if (nextDueDate == null)
									nextDueDate = date;
							} catch (ParseException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
								if (resp != null) {
									e.printStackTrace(resp.getWriter());
								}
								card.UpdateStatus(String.format("Error parsing date: %s", cell.asText()), null);
								webClient.close();
								return 0;
							}
							if (date.compareTo(today.getTime()) <= 0) {
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
