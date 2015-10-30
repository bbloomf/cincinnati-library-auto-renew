<%@ page import="myapp.Config" %>
<%@ page import="myapp.LibraryCard" %>
<%@ page import="myapp.User" %>
<%@ page import="myapp.Util" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.googlecode.objectify.Ref" %>
<%@ page import="com.google.apphosting.api.ApiProxy" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>
<%@ page import="com.google.appengine.api.users.UserService" %>
<%@ page import="com.google.appengine.api.users.UserServiceFactory" %>

<%
SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd-yyyy");
User user = User.findOrCreate();
boolean isAdmin = User.isAdmin();
%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cincinnati Library Auto-Renew</title>
    <link rel="stylesheet" type="text/css" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"/>
    <link rel="stylesheet" type="text/css" href="//cdn.jsdelivr.net/bootstrap.daterangepicker/2/daterangepicker.css" />
    <link rel="shortcut icon" type="image/x-icon" href="/favicon.ico">

    <style>
.nowrap {
	white-space: nowrap;
}
.glyphicon.glyphicon-share-alt.icon-spin {
	transform-origin: bottom;
	top: -5px;
}
.icon-spin {
  -webkit-animation: spin 1000ms infinite linear;
  animation: spin 1000ms infinite linear;
}
@-webkit-keyframes spin {
  0% {
    -webkit-transform: rotate(0deg);
  }
  100% {
    -webkit-transform: rotate(359deg);
  }
}
@keyframes spin {
  0% {
    transform: rotate(0deg);
  }
  100% {
    transform: rotate(359deg);
  }
}
    </style>

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
        
        <h3>Library Cards</h3>
        
        <!-- <p>
          Edit your configuration below.
        </p> -->

<%
    // sender email
    String sender_email = "donotreply@" + ((String) ApiProxy.getCurrentEnvironment().getAttributes()
        .get("com.google.appengine.runtime.default_version_hostname")).replaceFirst("\\.appspot\\.com$",
            ".appspotmail.com");

    // retrieve the configuration from the datastore using Objectify
    String master_email = "";
    Config cfg = Config.load();
    if(cfg != null)
      master_email = cfg.master_email;
	List<LibraryCard> cards;
	if(isAdmin) {
		cards = LibraryCard.getAll();
	} else {
    	cards = user.getLibraryCards();
    }
    
    if (cards.isEmpty()) {
%>
    <div class="well">No library cards have been added yet :(</div>
<% } else { %>

    <div class="table-responsive">
    <table class="table table-striped table-bordered table-hover">
      <tr>
        <% if(isAdmin) { %>
        <th>User Email</th>
        <th>Last Login</th>
        <% } %>
        <th>Email</th>
        <th>Card Number</th>
        <th>Last Checked</th>
        <th>Last Status</th>
        <th>Next Item Due</th>
        <th>Action</th>
      </tr>
    
<%  for(LibraryCard card : cards) { %>
      <tr class="<%= isAdmin && card.user.get().email.equalsIgnoreCase(user.email) ? "info" : "" %>">
        <% if(isAdmin) { %>
        <td><%= card.user.get().email %></td>
        <td><span class="timeago" title="<%= card.user.get().last_login==null?"":Util.jsTime.format(card.user.get().last_login) %>"><%= card.user.get().last_login==null? "--" : card.user.get().last_login %></span></td>
        <% } %>
        <td><%= card.email %></td>
        <td><%= card.card_number %></td>
        <td><span class="timeago" title="<%= card.date_last_checked==null?"":Util.jsTime.format(card.date_last_checked) %>"><%= card.date_last_checked == null? "--" : card.date_last_checked %></span></td>
        <td class="status_cell"><%= card.last_status == null? "--" : card.last_status %></td>
        <td><%= card.date_next_due == null? "--" : dateFormat.format(card.date_next_due) %></td>
        <td class='nowrap'>
        	<button type="button" class="btn btn-default" data-toggle="modal" data-target="#modal-library-card" data-email="<%= card.email %>" data-card="<%= card.card_number %>" title="Edit Card"><span class="glyphicon glyphicon-pencil" aria-hidden="true"></button>
        	<button type="button" class="btn btn-danger" data-action="delete" <% if(isAdmin) { %>data-email="<%= card.user.get().email %>"<% } %> data-card="<%= card.card_number %>" title="Delete Card"><span class="glyphicon glyphicon-remove" aria-hidden="true"></button>
        	<button type="button" class="btn btn-primary" data-action="recheck" <% if(isAdmin) { %>data-email="<%= card.user.get().email %>"<% } %> data-card="<%= card.card_number %>" title="Renew Items Now"><span class="glyphicon glyphicon-refresh" aria-hidden="true"></span></button>
        	<button type="button" class="btn btn-default" data-action="recheck-vacation" <% if(isAdmin) { %>data-email="<%= card.user.get().email %>"<% } %> data-card="<%= card.card_number %>" title="Renew Items Due Through..."><span class="glyphicon glyphicon-share-alt" aria-hidden="true"></span></button>
        </td>
      </tr>
<% } %>
  </table>
  </div>
<% } %>

        <button type="button" class="btn btn-primary btn-lg" data-toggle="modal" data-target="#modal-library-card" data-action="add">Add Library Card</button>

        <br/><br/>
        <div class="master_email">Emails will come from <strong><%= sender_email %></strong></div>

      </div>

    </div><!-- /.container -->

    <!-- Modal -->
    <div class="modal fade" id="modal-library-card" tabindex="-1" role="dialog" aria-labelledby="library-card-modal-label">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          
          <form action="updateconfig" method="post">
            <div class="modal-header">
              <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
              <h4 class="modal-title" id="library-card-modal-label">Library Card</h4>
            </div>
            <div class="modal-body">
              
              <div class="form-group">
                <label for="email">Email Address</label>
                <input type="email" class="form-control" placeholder="Email" id="email" name="email" autocomplete="false">
              </div>
              <div class="form-group">
                <label for="card_number">Card Number</label>
                <input type="text" class="form-control" placeholder="Card Number" id="card_number" name="card_number" autocomplete="false">
              </div>
              <div class="form-group">
                <label for="pin">Pin</label>
                <input type="password" class="form-control" placeholder="Pin" id="pin" name="pin" autocomplete="false">
              </div>

              <input type="hidden" id="action" name="action">

            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
              <button type="submit" class="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <!-- Modal -->
    <div class="modal fade" id="modal-master-email" tabindex="-1" role="dialog" aria-labelledby="master-email-label">
      <div class="modal-dialog" role="document">
        <div class="modal-content">
          <form action="updateconfig" method="post">
            <div class="modal-header">
              <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
              <h4 class="modal-title" id="master-email-label">Update Master Email</h4>
            </div>
            <div class="modal-body">
              
              <div class="form-group">
                <label for="email">Master Email Address</label>
                <input type="email" class="form-control" placeholder="Email" id="master_email" name="email" value="<%= master_email %>" autocomplete="false">
              </div>

              <input type="hidden" id="action" name="action" value="master_email">
            </div>
            <div class="modal-footer">
              <button type="button" class="btn btn-default" data-dismiss="modal">Close</button>
              <button type="submit" class="btn btn-primary">Save</button>
            </div>
          </form>
        </div>
      </div>
    </div>

    <script src="https://code.jquery.com/jquery-2.1.4.min.js"></script>
    <script src="//cdn.jsdelivr.net/momentjs/latest/moment.min.js"></script>
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-timeago/1.4.3/jquery.timeago.min.js"></script>
    <script src="//cdn.jsdelivr.net/bootstrap.daterangepicker/2/daterangepicker.js"></script>
    <script type="text/javascript">
      $(function() {
        // color errored cells
        $("td.status_cell").each(function(idx, ele) {
          var td = $(ele);
          if(td.text().toLowerCase().indexOf("error") > -1)
            td.closest("tr").addClass("danger");
        });

        // setup timeago for last check dates
        $.timeago.settings.localeTitle = true;
        $("span.timeago").timeago();
      });

      $("[data-action=delete]").click(function() {
        var card_number = $(this).data("card");
        if(confirm("Are you sure you want to delete this card ("+card_number+")?")) {
          var form = $("#modal-library-card form");
          form.find("#action").val("delete");
          form.find("#card_number").val(card_number);
<% if(isAdmin) { %>
          form.find("#email").val($(this).data("email"));
<% } %>
          form.submit();
        }
      });

      $("[data-action=recheck]").click(function() {
        var card_number = $(this).data("card");
        var email = $(this).data("email");
        var data = {
          card_number: card_number
        };
        if(email) data.email = email;
        $("button").attr("disabled", "disabled");
        $(this).find(".glyphicon").addClass("icon-spin");
        $.ajax({
          type: "GET",
          url: "/autorenew",
          data: data,
          success: function(data) {
            // refresh the page to update the table
            location.reload();
          }
        });
      });
      
      $("[data-action=recheck-vacation]").daterangepicker({
      	singleDatePicker: true,
      	minDate: 'today'
      }).on('apply.daterangepicker', function(ev, picker) {
	      var card_number = $(this).data("card");
        var email = $(this).data("email");
        var data = {
          card_number: card_number,
          vacation_ends: picker.startDate.format("YYYY-MM-DD") 
        };
        if(email) data.email = email;
        $('button').attr("disabled", "disabled");
        $(this).find(".glyphicon").addClass("icon-spin");
        $.ajax({
          type: "GET",
          url: "/autorenew",
          data: data,
          success: function(data) {
            // refresh the page to update the table
            location.reload();
          }
        });
		  });

      $("#modal-library-card").on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var type = button.data("action");

        var email = button.data("email") || "<%= user.email %>";
        var card_number = button.data("card");
        var pin = "";

        var modal = $(this);
        modal.find(".modal-title").text(type == "add" ? "Add New Card" : "Update Card");
        if(type == "add") {
          modal.find("#card_number").removeAttr("readonly");
        } else {
          modal.find("#card_number").attr("readonly", "readonly");
		}
        modal.find("#email").val(email);
        modal.find("#card_number").val(card_number)
        modal.find("#pin").val(pin);
      });
      $("#modal-library-card").on('shown.bs.modal', function (event) {
      	var button = $(event.relatedTarget);
      	var modal = $(this);
      	var card_number = button.data("card");
        if(card_number) {
        	modal.find("#pin").focus();
        } else {
        	modal.find("#card_number").focus();
        }
      });

      $(".master_email").click(function() {
        $("#modal-master-email").modal('show');
      });
    </script>
  </body>
</html>
