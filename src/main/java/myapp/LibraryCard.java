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
public class LibraryCard {
  @Id public String card_number;
  public String pin;
  public String email;

  public Date date_last_checked;
  public String last_status;

  public LibraryCard() {
  }

  public LibraryCard(String card_number, String pin, String email) {
    this.card_number = card_number;
    this.pin = pin;
    this.email = email;
  }

  public void UpdateStatus(String status) {
    date_last_checked = new Date();
    last_status = status;
  }

}