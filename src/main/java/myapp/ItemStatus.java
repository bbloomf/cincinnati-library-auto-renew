package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.gargoylesoftware.htmlunit.html.HtmlPage;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Ignore;

/**
 * The @Entity tells Objectify about our entity.  We also register it in {@link OfyHelper}
 * Our primary key @Id is set automatically by the Google Datastore for us.
 *
 * We add a @Parent to tell the object about its ancestor. We are doing this to support many
 * guestbooks.  Objectify, unlike the AppEngine library requires that you specify the fields you
 * want to index using @Index.  Only indexing the fields you need can lead to substantial gains in
 * performance -- though if not indexing your data from the start will require indexing it later.
 *
 * NOTE - all the properties are PUBLIC so that can keep the code simple.
 **/
@Entity
public class ItemStatus {
  @Id public String text;
  public Boolean worthTryingToRenew;
  @Ignore boolean justCreated = false;

  public ItemStatus() {
  }

  public ItemStatus(String text, Boolean worthTryingToRenew) {
    this.text = text;
    this.worthTryingToRenew = worthTryingToRenew;
    this.justCreated = true;
  }
  
  static {
	  ObjectifyService.register(ItemStatus.class);
  }
  
  public static ItemStatus findOrCreate(String text, Boolean worthTryingToRenew) {
	  return findOrCreate(text, worthTryingToRenew, null);
  }
  
  public static ItemStatus findOrCreate(String text, HtmlPage page) {
	  return findOrCreate(text, true, page);
  }
  public static ItemStatus findOrCreate(String text, Boolean worthTryingToRenew, HtmlPage page) {
	  ItemStatus status = ofy().load().type(ItemStatus.class).id(text).now();
	  if(status == null) {
		  status = new ItemStatus(text, worthTryingToRenew);
		  ofy().save().entity(status).now();
		  if(page != null) LibraryRenewer.email(null, "New item status created", String.format("The new item status '%s' has been created and is defaulting to triggering additional renew attempts.\n\n\n%s", text, page.asXml()));
	  }
	  return status;
  }
}