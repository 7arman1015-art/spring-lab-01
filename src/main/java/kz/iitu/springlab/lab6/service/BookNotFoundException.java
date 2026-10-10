package kz.iitu.springlab.lab6.service;

public class BookNotFoundException extends RuntimeException {

    private final long bookId;

    public BookNotFoundException(long bookId) {
        super("Book with id " + bookId + " was not found");
        this.bookId = bookId;
    }

    public long getBookId() {
        return bookId;
    }
}
