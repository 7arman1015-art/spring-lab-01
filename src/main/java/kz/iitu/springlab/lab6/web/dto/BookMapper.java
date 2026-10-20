package kz.iitu.springlab.lab6.web.dto;

import java.util.List;

import kz.iitu.springlab.lab6.domain.Author;
import kz.iitu.springlab.lab6.domain.Book;
import org.springframework.stereotype.Component;

@Component
public class BookMapper {

    public Book toDomain(BookRequest request) {
        Book book = new Book(request.title().strip(), request.genre(), request.year());
        book.setAuthor(new Author(request.author().strip(), "Unknown"));
        book.setAvailable(request.available() == null || request.available());
        return book;
    }

    public BookResponse toResponse(Book book) {
        return new BookResponse(book.id(), book.title(), book.author(), book.year(), book.genre(),
                book.getCreatedAt(), book.available(), book.isArchived());
    }

    public List<BookResponse> toResponseList(List<Book> books) {
        return books.stream().map(this::toResponse).toList();
    }
}
