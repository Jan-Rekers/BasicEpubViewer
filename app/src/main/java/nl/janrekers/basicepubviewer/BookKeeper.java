package nl.janrekers.basicepubviewer;

import nl.siegmann.epublib.domain.Book;

// The purpose of the BookKeeper is to store the book and the current bookmark separate from the BookViewActivity
// This is necessary as the user may turn the orientation of the device; in that case Android kills the activity and creates a new one
// You do not want to reload the book and loose the current bookmark in that case.

public class BookKeeper {
    private static BookKeeper singletonInstance = null;
    public static BookKeeper getInstance() {
        if (singletonInstance == null) {
            singletonInstance = new BookKeeper();
        }
        return singletonInstance;
    }

    private Book book;
    private boolean bookIsAvailable;
    private String currentResourceId;

    private BookKeeper() {
        bookIsAvailable = false;
    }

    public void loadBook(Book book) {
        this.book = book;
        currentResourceId = book.getSpine().getResource(0).getId();
        bookIsAvailable = true;
    }

    public void setCurrentResourceId(String resId) {
        currentResourceId = resId;
        // Log.i(this.getClass().getSimpleName(), "resource id: " + currentResourceId);
    }
    public Book getBook() {
        return bookIsAvailable ? book : null;
    }
    public String getCurrentResourceId() {
        return currentResourceId;
    }
    public boolean isBookIsAvailable() {
        return bookIsAvailable;
    }
}
