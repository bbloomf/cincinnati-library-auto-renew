<%@ page import="myapp.Config" %>
<%@ page import="myapp.LibraryCard" %>
<%@ page import="myapp.OfyService" %>
<%@ page import="com.googlecode.objectify.Key" %>
<%@ page import="com.google.apphosting.api.ApiProxy" %>

<%@ page import="java.util.Date" %>
<%@ page import="java.util.List" %>
<%@ page import="java.text.SimpleDateFormat" %>

<%
SimpleDateFormat jsTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'z'");
%>
<!DOCTYPE html>
<html lang="en">
  <head>
    <meta charset="utf-8">
    <meta http-equiv="X-UA-Compatible" content="IE=edge">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Cincinnati Library Auto-Renew</title>
    <link rel="stylesheet" type="text/css" href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/css/bootstrap.min.css"/>
    <link rel="shortcut icon" type="image/x-icon" href="favicon.ico">

    <style>
      .icon-spin {
        -webkit-animation: spin 1000ms infinite linear;
        animation: spin 1000ms infinite linear;
      }
      @-webkit-keyframes spin {
        0% {
          -webkit-transform: rotate(0deg);
          transform: rotate(0deg);
        }
        100% {
          -webkit-transform: rotate(359deg);
          transform: rotate(359deg);
        }
      }
      @keyframes spin {
        0% {
          -webkit-transform: rotate(0deg);
          transform: rotate(0deg);
        }
        100% {
          -webkit-transform: rotate(359deg);
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
    Config cfg = OfyService.ofy().load().type(Config.class).id(1).now();
    if(cfg != null)
      master_email = cfg.master_email;

    List<LibraryCard> cards = OfyService.ofy()
          .load()
          .type(LibraryCard.class)
          .list();
    
    if (cards.isEmpty()) {
%>
    <div class="well">No library cards have been added yet :(</div>
<% } else { %>

    <div class="table-responsive">
    <table class="table table-striped table-bordered table-hover">
      <tr>
        <th>Email</th>
        <th>Card Number</th>
        <th>Last Checked</th>
        <th>Last Status</th>
        <th>Action</th>
      </tr>
    
<%  for(LibraryCard card : cards) { %>
      <tr>
        <td><%= card.email %></td>
        <td><%= card.card_number %></td>
        <td><span class="timeago" title="<%= card.date_last_checked==null?"":jsTime.format(card.date_last_checked) %>"><%= card.date_last_checked == null? "--" : card.date_last_checked %></span></td>
        <td class="status_cell"><% if(card.last_status == null) { out.print("--"); } else { out.print(card.last_status); } %></td>
        <td><button type="button" class="btn btn-default" data-toggle="modal" data-target="#modal-library-card" data-email="<%= card.email %>" data-card="<%= card.card_number %>" title="Edit Card"><span class="glyphicon glyphicon-pencil" aria-hidden="true"></button> <button type="button" class="btn btn-danger" data-action="delete" data-card="<%= card.card_number %>" title="Delete Card"><span class="glyphicon glyphicon-remove" aria-hidden="true"></button> <button type="button" class="btn btn-primary" data-action="recheck" data-card="<%= card.card_number %>" title="Recheck"><span class="glyphicon glyphicon-refresh" aria-hidden="true"></span></button></td>
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
          
          <form action="/updateconfig" method="post">
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
          <form action="/updateconfig" method="post">
            <div class="modal-header">
              <button type="button" class="close" data-dismiss="modal" aria-label="Close"><span aria-hidden="true">&times;</span></button>
              <h4 class="modal-title" id="master-email-label">Update Master Email</h4>
            </div>
            <div class="modal-body">
              
              <div class="form-group">
                <label for="email">Master Email Address</label>
                <input type="email" class="form-control" placeholder="Email" id="email" name="email" value="<%= master_email %>" autocomplete="false">
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
    <script src="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js"></script>
    <script src="https://cdnjs.cloudflare.com/ajax/libs/jquery-timeago/1.4.3/jquery.timeago.min.js"></script>
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

      $("[data-action='delete']").click(this, function() {
        if(confirm("Are you sure you want to delete this card?")) {
          var card_number = $(this).data("card");
          var form = $("#modal-library-card form");
          form.find("#action").val("delete");
          form.find("#card_number").val(card_number);
          form.submit();
        }
      });

      $("[data-action='recheck']").click(this, function() {
        var card_number = $(this).data("card");

        $("button").attr("disabled", "disabled");
        $(this).find(".glyphicon").addClass("icon-spin");

        $.ajax({
          type: "GET",
          url: "/autorenew",
          data: {
            card_number: card_number
          },
          success: function(data) {
            // refresh the page to update the table
            location.reload();
          }
        });
      });

      $("#modal-library-card").on('show.bs.modal', function (event) {
        var button = $(event.relatedTarget);
        var type = button.data("action");

        var email = button.data("email");
        var card_number = button.data("card");
        var pin = "";

        var modal = $(this);
        modal.find(".modal-title").text(type == "add" ? "Add New Card" : "Update Card");
        if(type == "add")
          modal.find("card_number").removeAttr("readonly");
        else
          modal.find("card_number").attr("readonly", "readonly");

        modal.find("#email").val(email);
        modal.find("#card_number").val(card_number);
        modal.find("#pin").val(pin);
      });

      $(".master_email").click(function() {
        $("#modal-master-email").modal('show');
      });
    </script>
  </body>
</html>
