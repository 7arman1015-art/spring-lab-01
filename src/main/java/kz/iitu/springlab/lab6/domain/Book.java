package kz.iitu.springlab.lab6.domain;

public record Book(long id, String title, String author, int year,
                   Genre genre, boolean available) {
}
