package kz.iitu.springlab.lab6.service;

import java.util.List;

import kz.iitu.springlab.lab6.domain.Author;
import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.repository.AuthorRepository;
import kz.iitu.springlab.lab6.repository.BookRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class BookService {

    private final BookRepository books;
    private final AuthorRepository authors;

    public BookService(BookRepository books, AuthorRepository authors) {
        this.books = books;
        this.authors = authors;
    }

    public List<Book> findAll() {
        return books.findAllWithAuthor();
    }

    public Book findById(long id) {
        return books.findById(id).orElseThrow(() -> new BookNotFoundException(id));
    }

    @Transactional
    public Book create(Book draft) {
        return create(draft.title(), draft.author(), draft.year(), draft.genre(), draft.available());
    }

    @Transactional
    public Book create(String title, String author, int year, Genre genre, boolean available) {
        String normalizedTitle = title.strip();
        String normalizedAuthor = author.strip();
        if (books.existsByTitleAndAuthor_Name(normalizedTitle, normalizedAuthor)) {
            throw new DuplicateTitleException(normalizedTitle, normalizedAuthor);
        }
        Book book = new Book(normalizedTitle, genre, year);
        book.setAvailable(available);
        resolveAuthor(normalizedAuthor).addBook(book);
        return save(book);
    }

    @Transactional
    public Book update(long id, Book draft) {
        return update(id, draft.title(), draft.author(), draft.year(), draft.genre(), draft.available());
    }

    @Transactional
    public Book update(long id, String title, String author, int year, Genre genre, boolean available) {
        Book book = findById(id);
        String normalizedTitle = title.strip();
        String normalizedAuthor = author.strip();
        if (books.existsByTitleAndAuthor_NameAndIdNot(normalizedTitle, normalizedAuthor, id)) {
            throw new DuplicateTitleException(normalizedTitle, normalizedAuthor);
        }
        book.setTitle(normalizedTitle);
        book.setGenre(genre);
        book.setYear(year);
        book.setAvailable(available);
        if (!book.author().equals(normalizedAuthor)) {
            // The owning side updates author_id; removing from the old inverse
            // collection would orphan-delete the book during reassignment.
            resolveAuthor(normalizedAuthor).addBook(book);
        }
        return save(book);
    }

    @Transactional
    public void delete(long id) {
        books.delete(findById(id));
    }

    public Page<Book> find(Genre genre, Pageable pageable) {
        return genre == null ? books.findAll(pageable) : books.findByGenre(genre, pageable);
    }

    @Transactional
    public int archiveOlderThan(int year) {
        return books.archiveOlderThan(year);
    }

    private Author resolveAuthor(String name) {
        return authors.findByName(name)
                .orElseGet(() -> authors.save(new Author(name, "Unknown")));
    }

    private Book save(Book book) {
        try {
            return books.saveAndFlush(book);
        } catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause.getMessage() != null && cause.getMessage().contains("uk_book_title_author")) {
                    throw new DuplicateTitleException(book.title(), book.author());
                }
            }
            throw failure;
        }
    }
}
