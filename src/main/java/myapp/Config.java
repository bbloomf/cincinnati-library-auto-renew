package myapp;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyService;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.lang.String;
import java.util.List;

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
public class Config {
  @Id public Long id;
  
  public String master_email;

  public Config() {
    id = 1l;
  }
  
  static {
	  ObjectifyService.register(Config.class);
	  ItemStatus.findOrCreate("CANNOT RENEW - Item is on hold", true);
  }
  
  public static Config load() {
	  return ofy().load().type(Config.class).id(1).now();
  }

}