package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.Date;
import java.util.List;

import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.Ref;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
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
public class Vacation {
  @Id public Long id;
  @Index public Date startDate;
  @Index public Date endDate;
  @Load @Parent public Ref<User> user;
  
  public Vacation() {
  }

  public Vacation(User user, Date startDate, Date endDate) {
	this.user = Ref.create(user);
    this.startDate = startDate;
    this.endDate = endDate;
  }
  
  public static Vacation find(User user,Long id) {
	  return ofy().load().type(Vacation.class).parent(user).id(id).now();
  }

  static {
	  ObjectifyService.register(Vacation.class);
	  ObjectifyService.register(User.class);
  }
}