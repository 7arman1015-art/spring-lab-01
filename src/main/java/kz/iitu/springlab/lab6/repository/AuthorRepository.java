package kz.iitu.springlab.lab6.repository;

import java.util.Optional;

import kz.iitu.springlab.lab6.domain.Author;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthorRepository extends JpaRepository<Author, Long> {

    Optional<Author> findByName(String name);
}
