package myapp;

import java.io.IOException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;

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
        String email = req.getParameter("email");
        String vacationEndsString = req.getParameter("vacation_ends");
        Date vacationEnds = null;
        if(vacationEndsString != null) {
        	try {
				vacationEnds = Util.simpleDate.parse(vacationEndsString);
			} catch (ParseException e) {
				System.out.printf("ParseException: %s", e.toString());
				e.printStackTrace();
			}
        }
        UserService userService = UserServiceFactory.getUserService();
        User user = null;
        List<LibraryCard> cards = null;
        if(userService.isUserLoggedIn()) {
        	if(!userService.isUserAdmin() || email == null) {
        		email = userService.getCurrentUser().getEmail().toLowerCase();
        	}
        	user = User.find(email);
        	if(user == null) {
        		System.err.printf("email '%s' not found; aborting\n", email);
				return;
        	}
        	System.out.printf("Renewing items for user '%s'\n", user.email);
        }
        if(card_filter != null) {
        	if(user != null) {
	        	cards = new ArrayList<LibraryCard>(1);
	        	LibraryCard card = user.getLibraryCard(card_filter);
	        	if(card == null) {
	        		System.err.printf("card '%s' not found for user '%s'; aborting\n", card_filter, user.email);
					return;
	        	}
	            cards.add(card);
        	} else {
        		System.err.println("card_filter is only supported when logged in.");
        		return;
        	}
        } else {
        	if(user != null) {
        		cards = user.getLibraryCards();
        	} else {
        		cards = LibraryCard.getAll();
        	}
        }
        
        if(cards == null || cards.isEmpty()) {
            resp.getWriter().printf("No library cards have been added.\n");
        } else {
            for(LibraryCard card : cards) {
        	    System.out.printf("Renewing items for %s (%s)\n", card.user.get().email, card.card_number);
                resp.getWriter().printf("Renewing items for %s (%s)\n", card.user.get().email, card.card_number);
                LibraryRenewer.renew(card, false, vacationEnds, resp);
            }
        }

    }
}
