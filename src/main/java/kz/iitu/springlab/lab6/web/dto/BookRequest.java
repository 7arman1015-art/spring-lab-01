package kz.iitu.springlab.lab6.web.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.validation.NoDigits;

public record BookRequest(
        @NotBlank @Size(max = 120) String title,
        @NotBlank @Size(max = 160) @NoDigits String author,
        @NotNull @Min(1450) @Max(2100) Integer year,
        @NotNull Genre genre,
        Boolean available) {
}
