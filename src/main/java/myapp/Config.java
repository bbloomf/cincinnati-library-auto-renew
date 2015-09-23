package myapp;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Parent;
import com.googlecode.objectify.Key;

import java.lang.String;
import java.util.Date;

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
  
  public String card_number;
  public String pin;
  public String email;
  
  public String sender_email;

  public Date date_updated;

  public Config() {
  	id = 1l;
    date_updated = new Date();
  }

  public Config(String card_number, String pin, String email, String sender_email) {
    this();
    this.card_number = card_number;
    this.pin = pin;
	this.email = email;
    
    this.sender_email = sender_email;
  }

}