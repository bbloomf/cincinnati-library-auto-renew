# cincinnati-library-auto-renew
This is a project designed to run once a day and renew any books that a person has checked out from [the Public Library of Cincinnati and Hamilton County](http://cincinnatilibrary.org/) that are due at runtime.

Google App Engine
-----------------
It is designed to run from [Google App Engine](https://cloud.google.com/appengine/docs), and makes use of [HtmlUnit](http://htmlunit.sourceforge.net/) to interact with the library's website.

To Do
-----
* Add a feature to renew items due through a specific date for times when planning to be out of town later on