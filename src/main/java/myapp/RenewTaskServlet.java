package myapp;

import java.io.IOException;
import java.util.ArrayList;
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
		String email = req.getParameter("email");

		User user = null;
		if(email == null) {
			System.err.println("'email' is a required parameter and was not found; aborting");
			return;
		} else {
			user = User.find(email);
			if(user == null) {
				System.err.printf("email '%s' not found; aborting\n", email);
				return;
			}
		}
		// retrieve the cards from the datastore using Objectify
		LibraryCard card = null;
		if (card_filter == null) {
			System.err.println("'card_filter' is a required parameter and was not found; aborting");
			return;
		} else {
			card = LibraryCard.find(user, card_filter);
			if (card == null) {
				resp.getWriter().printf("Library card not found for user '%s', with card number '%s'.\n",user.email,card_filter);
				return;
			}
		}
		System.out.printf("Renewing items for %s (%s)\n", user.email, card.card_number);
		resp.getWriter().printf("Renewing items for %s (%s)\n", user.email, card.card_number);
		int tryToRenewCount = LibraryRenewer.renewTask(card, resp);
		// if we set the status to an error, it will keep retrying the
		// task.
		resp.setStatus(tryToRenewCount == 0 ? 200 : 500);
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
