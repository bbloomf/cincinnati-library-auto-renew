package myapp;

import java.text.SimpleDateFormat;

import com.google.appengine.api.taskqueue.Queue;
import com.google.appengine.api.taskqueue.QueueFactory;
import com.google.appengine.api.taskqueue.TaskOptions;
import com.google.apphosting.api.ApiProxy;

public class Util {
	public static String getFromEmail() {
		return "donotreply@" + ((String) ApiProxy.getCurrentEnvironment().getAttributes()
				.get("com.google.appengine.runtime.default_version_hostname")).replaceFirst("\\.appspot\\.com$",
						".appspotmail.com");
	}
	
	public static void enqueueTask(String email, String cardNumber) {
		Queue queue = QueueFactory.getDefaultQueue();
		queue.add(TaskOptions.Builder.withUrl("/renewTask").param("email",email).param("card_number", cardNumber).countdownMillis(15 * 60 * 1000));
	}
	
	public static SimpleDateFormat jsTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'z'");
	public static SimpleDateFormat libraryDateFormat = new SimpleDateFormat("MM-dd-yy");
	public static SimpleDateFormat simpleDate = new SimpleDateFormat("yyyy-MM-dd");
}
