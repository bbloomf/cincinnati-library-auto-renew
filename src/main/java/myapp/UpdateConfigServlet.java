package myapp;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;

import java.io.IOException;
import java.util.Date;
import java.util.List;

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

    String action = req.getParameter("action");
    String card_number = req.getParameter("card_number");

    if(action.equals("master_email")) {
      Config cfg = OfyService.ofy().load().type(Config.class).id(1).now();
      if(cfg == null)
        cfg = new Config();
      cfg.master_email = req.getParameter("email");
      OfyService.ofy().save().entity(cfg).now();
    } else if(action.equals("delete")) {
      OfyService.ofy().delete().type(LibraryCard.class).id(card_number).now();
    } else {

      String pin = req.getParameter("pin");
      String email = req.getParameter("email");
      
      // check if the library card exists
      LibraryCard card = OfyService.ofy().load().type(LibraryCard.class).id(card_number).now();

      if(card == null) {
        card = new LibraryCard();
        card.card_number = card_number;
      }

      card.email = email;
      if(pin != null && pin != "")  // only update the pin if it was set
        card.pin = pin;
      
      // // Use Objectify to save the Config and now() is used to make the call synchronously as we
      // // will immediately get a new page using redirect and we want the data to be present.
      OfyService.ofy().save().entity(card).now();
    }

    resp.sendRedirect("/index.jsp");
  }
}