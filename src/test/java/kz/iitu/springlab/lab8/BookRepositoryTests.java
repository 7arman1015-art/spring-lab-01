package kz.iitu.springlab.lab8;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.lab6.domain.Author;
import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.repository.BookRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace.NONE;

@DataJpaTest
@AutoConfigureTestDatabase(replace = NONE)
@ActiveProfiles("test")
class BookRepositoryTests {

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private BookRepository books;

    @BeforeEach
    void persistFixtures() {
        Author author = new Author("Repository Test Author", "Kazakhstan");
        entityManager.persist(author);
        persist(author, "Java Basics", Genre.TECHNOLOGY, 2001);
        persist(author, "Advanced JAVA", Genre.TECHNOLOGY, 2019);
        persist(author, "Java in History", Genre.HISTORY, 2020);
        persist(author, "Clean Architecture", Genre.TECHNOLOGY, 2017);
        persist(author, "Algorithms", Genre.TECHNOLOGY, 2014);
        persist(author, "Astronomy", Genre.SCIENCE, 2022);
        entityManager.flush();
        entityManager.clear();
    }

    @Test
    void derivedTitleQueryMatchesMixedCaseAcrossGenres() {
        assertThat(books.findByTitleContainingIgnoreCase("jAvA"))
                .extracting(Book::title)
                .containsExactlyInAnyOrder("Java Basics", "Advanced JAVA", "Java in History");
    }

    @Test
    void jpqlSearchFiltersGenreAndMinimumYearThenOrdersByTitle() {
        assertThat(books.search(Genre.TECHNOLOGY, 2010))
                .extracting(Book::title)
                .containsExactly("Advanced JAVA", "Algorithms", "Clean Architecture");
    }

    @Test
    void pageableGenreQueryReturnsSecondSortedPageWithFilteredTotal() {
        Page<Book> page = books.findByGenre(Genre.TECHNOLOGY,
                PageRequest.of(1, 2, Sort.by(Sort.Direction.DESC, "year")));

        assertThat(page.getNumber()).isEqualTo(1);
        assertThat(page.getSize()).isEqualTo(2);
        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getTotalPages()).isEqualTo(2);
        assertThat(page.isLast()).isTrue();
        assertThat(page.getContent()).extracting(Book::title)
                .containsExactly("Algorithms", "Java Basics");
        assertThat(page.getContent()).extracting(Book::year).containsExactly(2014, 2001);
    }

    private void persist(Author author, String title, Genre genre, int year) {
        Book book = new Book(title, genre, year);
        author.addBook(book);
        entityManager.persist(book);
    }
}
