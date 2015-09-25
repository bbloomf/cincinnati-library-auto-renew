package myapp;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class RenewTaskServlet extends HttpServlet {
	/**
	 * 
	 */
	
	private static void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// check if we are only rechecking a single card
		String card_filter = req.getParameter("card_number");

		// retrieve the cards from the datastore using Objectify
		List<LibraryCard> cards = null;
		if (card_filter != null)
			cards = OfyService.ofy().load().type(LibraryCard.class).filter("card_number", card_filter).list();
		else {
			return;
		}

		if (cards == null || cards.isEmpty()) {
			resp.getWriter().printf("No library cards have been added.\n");
		} else {
			for (LibraryCard card : cards) {
				System.out.printf("Renewing items for %s (%s)\n", card.email, card.card_number);
				resp.getWriter().printf("Renewing items for %s (%s)\n", card.email, card.card_number);
				int failedCount = LibraryRenewer.renewTask(card, resp);
				// if we set the status to an error, it will keep retrying the
				// task.
				resp.setStatus(failedCount == 0 ? 200 : 500);
			}
		}
	}

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}
	
	@Override
	public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		handleRequest(req, resp);
	}
}
