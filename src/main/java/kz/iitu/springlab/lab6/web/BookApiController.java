package kz.iitu.springlab.lab6.web;

import java.util.List;

import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.service.BookService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/books")
public class BookApiController {

    private final BookService books;

    public BookApiController(BookService books) {
        this.books = books;
    }

    @GetMapping
    public List<Book> list() {
        return books.findAll();
    }

    @GetMapping("/{id}")
    public Book detail(@PathVariable long id) {
        return books.findById(id);
    }
}
