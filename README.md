# BasicEpubViewer
An android app in java which is a simple viewer for epub files, based on epublib

The library epublib (https://github.com/psiegman/epublib) allows to parse an epub file and use its contents. 
I have written a simple epub viewer, using this library, which demonstrates how it
can be used in an android app to actually view the contents of the epub file.

Epub is a compressed archive containing html, jpg, png, and css files with the contents of the book.
As it is html, we can use a webview to display it.

This is a quite basic epub viewer with the following properties:
- it is an android app, written in java
- it is based on epublib (https://github.com/psiegman/epublib)
- it displays the contents of the book in a webview
- it uses a ContentProvider to feed the contents of the epub file to the webview
- the user can navigate forward and backward through the book following the sections as defined in the epub file
- the app uses a non-UI thread (WorkManager & Worker) to read the epub file
- the epub files are read from a Books folder and the app requests permission to read this folder
- the read book is kept in a singleton separate from the activity
- the app does not keep the current bookmark within the book in a persistent manner, it always restarts at the first page
- the app does not split a section into separate pages

You may use this code as inspiration or as a basis for your own epub viewer.
It took me a few days to figure it all out, and I would have liked to have an example like this Viewer to guide me through.
