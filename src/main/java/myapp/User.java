package myapp;

import static com.googlecode.objectify.ObjectifyService.ofy;

import java.util.Date;
import java.util.List;

import com.google.appengine.api.users.UserService;
import com.google.appengine.api.users.UserServiceFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;

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
public class User {
  @Id public String email;
  public Date last_login;
  
  public User() {
  }

  public User(String email) {
    this.email = email;
    this.last_login = new Date();
  }
  
  public void setLoggedIn() {
	  last_login = new Date();
  }
  
  public List<LibraryCard> getLibraryCards() {
	  return ofy()
      .load()
      .type(LibraryCard.class)
      .ancestor(this)
      .list();
  }
  
  public LibraryCard getLibraryCard(String card_filter) {
	  return ofy().load().type(LibraryCard.class).parent(this).id(card_filter).now();
  }
  
  static {
	  ObjectifyService.register(User.class);
  }
  
  public static boolean isAdmin() {
	  UserService userService = UserServiceFactory.getUserService();
	  return userService.isUserLoggedIn() && userService.isUserAdmin();
  }
  
  public static User findOrCreate() {
	  return findOrCreate(UserServiceFactory.getUserService().getCurrentUser().getEmail().toLowerCase());
  }
  
  public static User findOrCreate(String email) {
	  User user = find(email);
	  if(user == null) {
	  	user = new User(email.toLowerCase());
	  } else {
	  	user.setLoggedIn();
	  }
	  ofy().save().entity(user).now();
	  return user;
  }
  
  public static User find(String email) {
	  return ofy().load().type(User.class).id(email).now();
  }
}