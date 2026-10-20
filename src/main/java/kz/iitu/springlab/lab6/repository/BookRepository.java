package kz.iitu.springlab.lab6.repository;

import java.util.List;
import java.util.Optional;

import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BookRepository extends JpaRepository<Book, Long> {

    @EntityGraph(attributePaths = "author")
    List<Book> findByGenre(Genre genre);

    List<Book> findByTitleContainingIgnoreCase(String fragment);

    List<Book> findByYearBetweenOrderByYearDesc(int from, int to);

    List<Book> findTop3ByGenreOrderByYearDesc(Genre genre);

    long countByAuthorName(String name);

    @Query("""
            select b from Book b
            where b.genre = :genre and b.year >= :year
            order by b.title
            """)
    List<Book> search(@Param("genre") Genre genre, @Param("year") int year);

    @Modifying(clearAutomatically = true)
    @Query("update Book b set b.archived = true where b.year < :year")
    int archiveOlderThan(@Param("year") int year);

    @Query("select b from Book b join fetch b.author order by b.id")
    List<Book> findAllWithAuthor();

    @Override
    @EntityGraph(attributePaths = "author")
    Optional<Book> findById(Long id);

    @Override
    @EntityGraph(attributePaths = "author")
    Page<Book> findAll(Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Page<Book> findByGenre(Genre genre, Pageable pageable);

    @EntityGraph(attributePaths = "author")
    Slice<Book> findSliceBy(Pageable pageable);

    boolean existsByTitleAndAuthor_Name(String title, String authorName);

    boolean existsByTitleAndAuthor_NameAndIdNot(String title, String authorName, Long id);
}
