package kz.iitu.springlab.lab6.service;

public class DuplicateTitleException extends RuntimeException {

    private final String title;
    private final String author;

    public DuplicateTitleException(String title, String author) {
        super("Book '" + title + "' by '" + author + "' already exists");
        this.title = title;
        this.author = author;
    }

    public String getTitle() { return title; }
    public String getAuthor() { return author; }
}
