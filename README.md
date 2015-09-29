# cincinnati-library-auto-renew
This is a project designed to run once a day and renew any books that a person has checked out from [the Public Library of Cincinnati and Hamilton County](http://cincinnatilibrary.org/) that are due at runtime.

Google App Engine
-----------------
It is designed to run from [Google App Engine](https://cloud.google.com/appengine/docs), and makes use of [HtmlUnit](http://htmlunit.sourceforge.net/) to interact with the library's website.

To Do
-----
* It should not keep trying to renew things that failed because the renewal limit was reached.
* It should still keep track of the next item due date when it has actually renewed things. 
* The main page should require a google login, and then only show the library cards associated with that email.