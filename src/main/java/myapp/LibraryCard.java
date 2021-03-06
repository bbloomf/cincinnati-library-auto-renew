package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.Date;
import java.util.List;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Load;
import com.googlecode.objectify.annotation.Parent;

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
public class LibraryCard {
  @Id public String card_number;
  @Load @Parent public Ref<User> user;
  public String pin;
  public String email;

  public Date date_last_checked;
  public Date date_next_due;
  public String last_status;

  public LibraryCard() {
  }

  public LibraryCard(User user, String card_number, String pin, String email) {
	this.user = Ref.create(user);
    this.card_number = card_number;
    this.pin = pin;
    this.email = email;
  }

  public void UpdateStatus(String status, Date date_next_due) {
    date_last_checked = new Date();
    last_status = status;
    this.date_next_due = date_next_due; 
    ofy().save().entity(this).now();
  }
  
  static {
	  ObjectifyService.register(LibraryCard.class);
	  ObjectifyService.register(User.class);
  }
  
  public static List<LibraryCard> getAll() {
	  return ofy().load().type(LibraryCard.class).list();
  }
  
  public static LibraryCard find(User user,String card_number) {
	  return ofy().load().type(LibraryCard.class).parent(user).id(card_number).now();
  }
}