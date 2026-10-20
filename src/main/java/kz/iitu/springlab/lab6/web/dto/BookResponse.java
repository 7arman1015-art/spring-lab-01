package kz.iitu.springlab.lab6.web.dto;

import java.time.Instant;

import kz.iitu.springlab.lab6.domain.Genre;

public record BookResponse(Long id, String title, String author, int year, Genre genre,
                           Instant createdAt, boolean available, boolean archived) {
}
