package myapp;

import java.io.IOException;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.apphosting.api.ApiProxy;

public class RenewServlet extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = -330119396067396L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/plain");
    	
        // check if we are only rechecking a single card
        String card_filter = req.getParameter("card_number");

        // retrieve the cards from the datastore using Objectify
        List<LibraryCard> cards = null;
        if(card_filter != null)
            cards = OfyService.ofy()
                .load()
                .type(LibraryCard.class)
                .filter("card_number", card_filter)
                .list();
        else {
            cards = OfyService.ofy()
              .load()
              .type(LibraryCard.class)
              .list();
        }

        if(cards == null || cards.isEmpty()) {
            resp.getWriter().printf("No library cards have been added.\n");
        } else {
            for(LibraryCard card : cards) {
                System.out.printf("Renewing items for %s (%s)\n", card.email, card.card_number);
                resp.getWriter().printf("Renewing items for %s (%s)\n", card.email, card.card_number);
                LibraryRenewer.renew(card, resp);
            }
        }

    }
}
