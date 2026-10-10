package kz.iitu.springlab.lab6.service;

import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import org.springframework.stereotype.Service;

@Service
public class BookService {

    private final ConcurrentHashMap<Long, Book> books = new ConcurrentHashMap<>();
    private final AtomicLong sequence = new AtomicLong();

    public BookService() {
        create("The Little Prince", "Antoine de Saint-Exupery", 1943, Genre.FICTION, true);
        create("A Brief History of Time", "Stephen Hawking", 1988, Genre.SCIENCE, true);
        create("Clean Code", "Robert Martin", 2008, Genre.TECHNOLOGY, false);
    }

    public List<Book> findAll() {
        return books.values().stream().sorted(Comparator.comparingLong(Book::id)).toList();
    }

    public Book findById(long id) {
        Book book = books.get(id);
        if (book == null) {
            throw new BookNotFoundException(id);
        }
        return book;
    }

    public Book create(String title, String author, int year, Genre genre, boolean available) {
        long id = sequence.incrementAndGet();
        Book book = new Book(id, title.strip(), author.strip(), year, genre, available);
        books.put(id, book);
        return book;
    }
}
