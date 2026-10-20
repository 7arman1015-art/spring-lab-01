package kz.iitu.springlab.lab6.web;

import java.net.URI;
import java.util.Set;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.service.BookService;
import kz.iitu.springlab.lab6.web.dto.BookMapper;
import kz.iitu.springlab.lab6.web.dto.BookRequest;
import kz.iitu.springlab.lab6.web.dto.BookResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Books", description = "PostgreSQL book catalogue with paging and sorting")
@RestController
@RequestMapping("/api/books")
public class BookApiController {

    private final BookService books;
    private final BookMapper mapper;
    private static final Set<String> SORT_FIELDS = Set.of("id", "title", "year", "genre", "createdAt", "author.name");

    public BookApiController(BookService books, BookMapper mapper) {
        this.books = books;
        this.mapper = mapper;
    }

    @GetMapping
    @Operation(summary = "Find a page of books", description = "Optional genre filter; default size 5, sorted by title. Maximum size 100.")
    public Page<BookResponse> list(@RequestParam(required = false) Genre genre,
                                  @PageableDefault(size = 5, sort = "title") Pageable pageable) {
        if (pageable.getPageSize() > 100) {
            throw new IllegalArgumentException("Page size must be between 1 and 100");
        }
        pageable.getSort().forEach(order -> {
            if (!SORT_FIELDS.contains(order.getProperty())) {
                throw new IllegalArgumentException("Unknown sort field: " + order.getProperty()
                        + ". Allowed fields: id, title, year, genre, createdAt, author.name");
            }
        });
        return books.find(genre, pageable).map(mapper::toResponse);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Find a book by its identifier")
    public BookResponse detail(@PathVariable long id) {
        return mapper.toResponse(books.findById(id));
    }

    @PostMapping
    @Operation(summary = "Create a book", description = "Returns 201 and the new resource Location; duplicates return 409.")
    public ResponseEntity<BookResponse> create(@Valid @RequestBody BookRequest request) {
        var saved = books.create(mapper.toDomain(request));
        return ResponseEntity.created(URI.create("/api/books/" + saved.id())).body(mapper.toResponse(saved));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Replace a book's editable fields")
    public BookResponse update(@PathVariable long id, @Valid @RequestBody BookRequest request) {
        return mapper.toResponse(books.update(id, mapper.toDomain(request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a book", description = "Returns 204; repeating deletion of the missing resource returns 404.")
    public ResponseEntity<Void> delete(@PathVariable long id) {
        books.delete(id);
        return ResponseEntity.noContent().build();
    }
}
