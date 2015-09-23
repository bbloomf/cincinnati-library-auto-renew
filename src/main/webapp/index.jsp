<%@ page import="myapp.Config" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.ObjectifyService" %>

<%@ page import="java.util.Date" %>

<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cincinnati Library Auto-Renew</title>
    <link rel="stylesheet" type="text/css" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"/>
    
    <!-- HTML5 shim and Respond.js for IE8 support of HTML5 elements and media queries -->
    <!--[if lt IE 9]>
      <script src="https://oss.maxcdn.com/html5shiv/3.7.2/html5shiv.min.js"></script>
      <script src="https://oss.maxcdn.com/respond/1.4.2/respond.min.js"></script>
    <![endif]-->
  </head>
  <body>

    <div class="container">

      <div class="starter-template">
        <h1>Cincinnati Library Auto-Renew</h1>
        
        <h3>Configuration</h3>
        
        <p>
          Edit your configuration below.
        </p>

<%
    // retrieve the configuration from the datastore using Objectify
    Config cfg = ObjectifyService.ofy()
          .load()
          .type(Config.class)
          .id(1)
          .now();

    String email = "";
    String card_number = "";
    String pin = "";
    String sender_email = "";
    Date date_updated = null;
    
    if (cfg == null) {
%>
    No configuration set :(
<% } else { %>
    Configuration found :)
<%
    email = cfg.email;
    card_number = cfg.card_number;
    pin = cfg.pin;
    sender_email = cfg.sender_email;
    date_updated = cfg.date_updated;
} %>

        <form action="/updateconfig" method="post">
          <div class="form-group">
            <label for="email">Email Address</label>
            <input type="email" class="form-control" placeholder="Email" id="email" name="email" value="<%= email %>">
          </div>
          <div class="form-group">
            <label for="card_number">Card Number</label>
            <input type="text" class="form-control" placeholder="Card Number" id="card_number" name="card_number" value="<%= card_number %>">
          </div>
          <div class="form-group">
            <label for="pin">Pin</label>
            <input type="text" class="form-control" placeholder="Pin" id="pin" name="pin" value="<%= pin %>">
          </div>

          <br/>
          <div class="form-group">
            <label for="sender_email">Sender Email Address</label>
            <input type="email" class="form-control" placeholder="Email" id="sender_email" name="sender_email" value="<%= sender_email %>">
          </div>

          <button type="submit" class="btn btn-default">Submit</button>

          <span><%= date_updated %></span>
        </form>

      </div>

    </div><!-- /.container -->

  </body>
</html>
