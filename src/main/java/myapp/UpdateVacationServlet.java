package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.io.IOException;
import java.text.ParseException;
import java.util.Date;

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
public class UpdateVacationServlet extends HttpServlet {

  // Process the http POST of the form
  @Override
  public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
	User user = User.findOrCreate();
	if(req.getParameter("delete") != null) {
		ofy().delete().type(Vacation.class).parent(user).id(Long.parseLong(req.getParameter("delete")));
		return;
	}
	String startString = req.getParameter("start");
    String endString = req.getParameter("end");
    Date start, end;
	try {
		start = Util.simpleDate.parse(startString);
	} catch (ParseException e) {
		System.out.println(e.toString());
		e.printStackTrace();
		return;
	}
    try {
		end = Util.simpleDate.parse(endString);
		end.setTime(end.getTime() + (1000 * 60 * 60 * 24) - 1);
	} catch (ParseException e) {
		System.out.println(e.toString());
		e.printStackTrace();
		return;
	}
    String sid = req.getParameter("id");
    Vacation v;
	if(sid != null) {
		Long id = Long.parseLong(sid);
		v = Vacation.find(user,id);
		v.startDate = start;
		v.endDate = end;
	} else {
		v = new Vacation(user, start, end);
	}
	ofy().save().entity(v).now();
  }
}