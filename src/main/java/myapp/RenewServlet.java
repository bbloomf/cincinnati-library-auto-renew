package myapp;

import java.io.IOException;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PreparedQuery;
import com.google.appengine.api.datastore.Query;

import com.googlecode.objectify.ObjectifyService;

public class RenewServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -330119396067396L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		// retrieve the configuration from the datastore using Objectify
		Config cfg = ObjectifyService.ofy()
			  .load()
			  .type(Config.class)
			  .id(1)
			  .now();

	  	if(cfg == null || cfg.email == null || cfg.card_number == null || cfg.pin == null) {
	  		resp.setContentType("text/plain");
			resp.getWriter().printf("Configuration not set.\n");
	  	} else {
			String email = cfg.email;
			String card = cfg.card_number;
			String pin = cfg.pin;
			String masterEmail = cfg.sender_email;

			System.out.printf("Renewing items for %s (%s)\n", email, card);
			resp.setContentType("text/plain");
			resp.getWriter().printf("Renewing items for %s (%s)\n", email, card);
			LibraryRenewer.renew(card, pin, email, masterEmail, resp);
		}

	}
}
