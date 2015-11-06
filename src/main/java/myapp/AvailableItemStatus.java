package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import com.google.apphosting.api.ApiProxy;
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
public class AvailableItemStatus {
  @Id public String text;
  public Boolean canBePutOnHold;
  @Ignore boolean justCreated = false;

  public AvailableItemStatus() {
  }

  public AvailableItemStatus(String text, Boolean canBePutOnHold) {
    this.text = text;
    this.canBePutOnHold = canBePutOnHold;
    this.justCreated = true;
  }
  
  static {
	  ObjectifyService.register(AvailableItemStatus.class);
  }
  
  public static AvailableItemStatus findOrCreate(String text) {
	  return findOrCreate(text, true);
  }
  public static AvailableItemStatus findOrCreate(String text, Boolean canBePutOnHold) {
	  AvailableItemStatus status = ofy().load().type(AvailableItemStatus.class).id(text).now();
	  if(status == null) {
		  status = new AvailableItemStatus(text, canBePutOnHold);
		  ofy().save().entity(status).now();
		  LibraryRenewer.email(null, "New Available Item status created", String.format("The new _available item status_ '%s' has been created and is defaulting to being thought of as able to be put on hold.\n\nSee here: https://console.developers.google.com/datastore/query?queryType=KindQuery&namespace=&kind=AvailableItemStatus&project=%s", 
				  text,
				  ((String)ApiProxy.getCurrentEnvironment().getAttributes().get("com.google.appengine.runtime.default_version_hostname")).
				  replaceFirst("\\.appspot\\.com$", "")
				  ));
	  }
	  return status;
  }
}