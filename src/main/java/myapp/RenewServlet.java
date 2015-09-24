package myapp;

import java.io.IOException;

import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

public class RenewServlet extends HttpServlet {
    /**
     * 
     */
    private static final long serialVersionUID = -330119396067396L;

    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Config cfg = OfyService.ofy().load().type(Config.class).first().now();
        String masterEmail = "";
        if(cfg != null)
            masterEmail = cfg.master_email;

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
            resp.setContentType("text/plain");
            resp.getWriter().printf("No library cards have been added.\n");
        } else {
            for(LibraryCard card : cards) {
                String email = card.email;
                String card_number = card.card_number;
                String pin = card.pin;
                
                System.out.printf("Renewing items for %s (%s)\n", email, card_number);
                resp.setContentType("text/plain");
                resp.getWriter().printf("Renewing items for %s (%s)\n", email, card_number);
                LibraryRenewer.renew(card_number, pin, email, masterEmail, resp);
            }
        }

    }
}
