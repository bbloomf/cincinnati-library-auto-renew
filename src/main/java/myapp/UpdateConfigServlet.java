package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

//import com.googlecode.objectify.ObjectifyService;

/**
 * Form Handling Servlet
 * Most of the action for this sample is in webapp/index.jsp, which displays the
 * {@link Config}'s. This servlet has one method
 * {@link #doPost(<#HttpServletRequest req#>, <#HttpServletResponse resp#>)} which takes the form
 * data and saves it.
 */
public class UpdateConfigServlet extends HttpServlet {

  // Process the http POST of the form
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	User user = User.findOrCreate();
	String email = req.getParameter("email");
    String action = req.getParameter("action");
    String card_number = req.getParameter("card_number");

    if(action.equals("master_email")) {
      if(User.isAdmin()) {
	      Config cfg = Config.load();
	      if(cfg == null)
	        cfg = new Config();
	      cfg.master_email = email;
	      ofy().save().entity(cfg).now();
      }
    } else if(action.equals("delete")) {
      if(User.isAdmin() && email != null) {
    	  user = User.find(email);
      }
      ofy().delete().type(LibraryCard.class).parent(user).id(card_number).now();
    } else {

      String pin = req.getParameter("pin");
      
      // check if the library card exists
      LibraryCard card = user.getLibraryCard(card_number);

      if(card == null) {
        card = new LibraryCard(user, card_number, pin, email);
      } else {
    	  card.email = email;
    	  if(pin != null && pin != "") { // only update the pin if it was set
	        card.pin = pin;
	      }
      }
      
      // // Use Objectify to save the Config and now() is used to make the call synchronously as we
      // // will immediately get a new page using redirect and we want the data to be present.
      ofy().save().entity(card).now();
    }

    resp.sendRedirect("/index.jsp");
  }
}