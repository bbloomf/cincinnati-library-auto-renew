package myapp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
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
import com.google.apphosting.api.ApiProxy;

public class LibraryRenewer {
	private static void email(String email, String subject, String body) {
		Config cfg = OfyService.ofy().load().type(Config.class).first().now();
        String masterEmail = null;
        if(cfg != null)
            masterEmail = cfg.master_email;

		String from = "donotreply@" + ((String) ApiProxy.getCurrentEnvironment().getAttributes()
				.get("com.google.appengine.runtime.default_version_hostname")).replaceFirst("\\.appspot\\.com$",
						".appspotmail.com");
		Properties props = new Properties();
		Session session = Session.getDefaultInstance(props, null);

		try {
			Message msg = new MimeMessage(session);
			msg.setFrom(new InternetAddress(from));
			msg.addRecipient(Message.RecipientType.TO, new InternetAddress(email));
			if (masterEmail != null && masterEmail != email) {
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

	private static void updateStatus(String card_number, String status) {
		// update datastore with current status
		LibraryCard card = OfyService.ofy().load().type(LibraryCard.class).id(card_number).now();
		card.UpdateStatus(status);
		OfyService.ofy().save().entity(card).now();
	}

	public static void renew(LibraryCard card)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
		renew(card, null);
	}

	public static void renew(LibraryCard card, HttpServletResponse resp)
			throws FailingHttpStatusCodeException, MalformedURLException, IOException {
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
				updateStatus(card.card_number, status);
				if (resp != null)
					resp.getWriter().println(status);
				webClient.close();
				return;
			}
		}

		HtmlTable table = (HtmlTable) page.getElementsByTagName("table").get(0);
		int rowId = 0;
		HashMap<Integer, String> column = new HashMap<Integer, String>();
		HashMap<String, Integer> columnId = new HashMap<String, Integer>();
		ArrayList<HashMap<String, HtmlTableCell>> rows = new ArrayList<HashMap<String, HtmlTableCell>>();
		Pattern p = Pattern.compile("DUE ((\\d+)-(\\d+)-(\\d+))(\\s+.*)?");
		SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yy");
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
				rows.add(workingRow = new HashMap<String, HtmlTableCell>());
			}
			for (final HtmlTableCell cell : row.getCells()) {
				if (rowId == 1) {
					column.put(colId, cell.asText().toLowerCase());
					columnId.put(cell.asText().toLowerCase(), colId);
				} else if (rowId > 1) {
					workingRow.put(column.get(colId), cell);
					if ("status".equalsIgnoreCase(column.get(colId))) {
						Matcher m = p.matcher(cell.asText());
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
								webClient.close();
								return;
							}
							// System.out.println(m.group(1));
							// System.out.println(date);
							// System.out.println(today.getTime());
							// System.out.println(date.compareTo(today.getTime()));
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
					status = String.format("Attempted to renew %s item%s\n", needToRenew, needToRenew == 1 ? "" : "s");
					email(card.email, status,
							"Please check the logs to make sure the renew was successful:\n" + page.asXml());
				} else {
					status = "No 'Yes' anchor";
				}
			} else {
				status = "No 'Renew Marked' anchor";
			}
		} else {
			status = "Nothing to renew";
			if (nextDueDate != null && needToRenew == 0) {
				status += String.format("; Next item is due on %s", dateFormat.format(nextDueDate));
			}
		}
		updateStatus(card.card_number, status);
		System.out.println(status);

		// System.out.println(page.getUrl().toString());
		// final String pageAsXml = page.asXml();
		// System.out.println(pageAsXml);
		webClient.close();
		if (resp != null) {
			resp.getWriter().println(status);
			resp.getWriter().println();
		}
	}
}
