package kz.iitu.springlab.lab6.config;

import java.util.Set;

import kz.iitu.springlab.lab6.domain.Author;
import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.domain.Tag;
import kz.iitu.springlab.lab6.repository.AuthorRepository;
import kz.iitu.springlab.lab6.repository.BookRepository;
import kz.iitu.springlab.lab6.repository.TagRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("dev")
public class DevelopmentData implements CommandLineRunner {

    private final BookRepository books;
    private final AuthorRepository authors;
    private final TagRepository tags;

    public DevelopmentData(BookRepository books, AuthorRepository authors, TagRepository tags) {
        this.books = books;
        this.authors = authors;
        this.tags = tags;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (books.count() != 0 || authors.count() != 0 || tags.count() != 0) {
            return;
        }
        Tag programming = tags.save(new Tag("programming"));
        Tag architecture = tags.save(new Tag("architecture"));
        Tag science = tags.save(new Tag("science"));
        Tag bestseller = tags.save(new Tag("bestseller"));

        Author martin = new Author("Robert Martin", "United States");
        Author bloch = new Author("Joshua Bloch", "United States");
        Author hawking = new Author("Stephen Hawking", "United Kingdom");

        Book cleanCode = add(martin, "Clean Code", Genre.TECHNOLOGY, 2008, programming, architecture);
        cleanCode.setAvailable(false);
        add(martin, "Clean Architecture", Genre.TECHNOLOGY, 2017, architecture);
        add(martin, "The Clean Coder", Genre.TECHNOLOGY, 2011, programming);
        add(bloch, "Effective Java", Genre.TECHNOLOGY, 2018, programming, bestseller);
        add(bloch, "Java Puzzlers", Genre.TECHNOLOGY, 2005, programming);
        add(bloch, "Java Concurrency in Practice", Genre.TECHNOLOGY, 2006, programming);
        add(hawking, "A Brief History of Time", Genre.SCIENCE, 1988, science, bestseller);
        add(hawking, "The Universe in a Nutshell", Genre.SCIENCE, 2001, science);

        authors.save(martin);
        authors.save(bloch);
        authors.save(hawking);
    }

    private Book add(Author author, String title, Genre genre, int year, Tag... bookTags) {
        Book book = new Book(title, genre, year);
        book.getTags().addAll(Set.of(bookTags));
        author.addBook(book);
        return book;
    }
}
