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

public class RenewServlet extends HttpServlet {
	/**
	 * 
	 */
	private static final long serialVersionUID = -330119396067396L;

	@Override
	public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Query q = new Query("User");
		PreparedQuery pq = datastore.prepare(q);
		for (Entity result : pq.asIterable()) {
			String email = (String) result.getProperty("email");
			String card = (String) result.getProperty("card");
			String pin = (String) result.getProperty("pin");

			System.out.printf("Renewing items for %s (%s)\n", email, card);
			resp.setContentType("text/plain");
			resp.getWriter().printf("Renewing items for %s (%s)\n", email, card);
			LibraryRenewer.renew(card, pin, email, resp);
		}

	}
}
