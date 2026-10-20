package kz.iitu.springlab.lab8;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import jakarta.persistence.EntityManager;
import kz.iitu.springlab.lab6.config.DevelopmentData;
import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.repository.AuthorRepository;
import kz.iitu.springlab.lab6.repository.BookRepository;
import kz.iitu.springlab.lab6.repository.TagRepository;
import kz.iitu.springlab.lab6.service.BookService;
import org.hibernate.Session;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class Lab8MeasurementsTests {

    @Autowired private EntityManager entityManager;
    @Autowired private BookRepository books;
    @Autowired private AuthorRepository authors;
    @Autowired private TagRepository tags;
    @Autowired private BookService service;
    @Autowired private MockMvc mvc;

    @Test
    void measureQueriesNPlusOnePagingAndApiAgainstPostgresql() throws Exception {
        emptyCatalogue();
        new DevelopmentData(books, authors, tags).run();
        entityManager.flush();
        entityManager.clear();

        List<Book> seed = books.findAll();
        assertThat(seed).hasSize(8);
        assertThat(authors.count()).isEqualTo(3);
        assertThat(tags.count()).isEqualTo(4);
        Map<String, Object> database = entityManager.unwrap(Session.class).doReturningWork(connection -> {
            var metadata = connection.getMetaData();
            return Map.<String, Object>of("product", metadata.getDatabaseProductName(),
                    "version", metadata.getDatabaseProductVersion(), "catalog", connection.getCatalog());
        });
        assertThat(database.get("product")).isEqualTo("PostgreSQL");

        Map<String, Object> evidence = new LinkedHashMap<>();
        evidence.put("captured_at", Instant.now().toString());
        evidence.put("database", database);
        evidence.put("test", getClass().getName() + ".measureQueriesNPlusOnePagingAndApiAgainstPostgresql");
        evidence.put("transaction", "All fixture and bulk changes roll back after this test.");
        evidence.put("dataset", Map.of("books", seed.size(), "authors", authors.count(), "tags", tags.count()));
        List<Map<String, Object>> queries = new java.util.ArrayList<>();

        Observation<List<Book>> genre = measure(() -> books.findByGenre(Genre.TECHNOLOGY));
        assertThat(genre.result()).hasSize(6);
        queries.add(bookQuery("findByGenre", Map.of("genre", "TECHNOLOGY"), genre));

        Observation<List<Book>> title = measure(() -> books.findByTitleContainingIgnoreCase("jAvA"));
        assertThat(title.result()).extracting(Book::title).containsExactlyInAnyOrder(
                "Effective Java", "Java Puzzlers", "Java Concurrency in Practice");
        queries.add(bookQuery("findByTitleContainingIgnoreCase", Map.of("fragment", "jAvA"), title));

        Observation<List<Book>> years = measure(() -> books.findByYearBetweenOrderByYearDesc(2000, 2010));
        assertThat(years.result()).extracting(Book::year).containsExactly(2008, 2006, 2005, 2001);
        queries.add(bookQuery("findByYearBetweenOrderByYearDesc", Map.of("from", 2000, "to", 2010), years));

        Observation<List<Book>> top = measure(() -> books.findTop3ByGenreOrderByYearDesc(Genre.TECHNOLOGY));
        assertThat(top.result()).extracting(Book::title)
                .containsExactly("Effective Java", "Clean Architecture", "The Clean Coder");
        queries.add(bookQuery("findTop3ByGenreOrderByYearDesc", Map.of("genre", "TECHNOLOGY"), top));

        Observation<Long> count = measure(() -> books.countByAuthorName("Robert Martin"));
        assertThat(count.result()).isEqualTo(3L);
        assertThat(count.sql()).hasSize(1);
        queries.add(Map.of("method", "countByAuthorName", "parameters", Map.of("name", "Robert Martin"),
                "returned_rows", 1, "result_value", count.result(), "sql_count", count.sql().size(), "sql", count.sql()));

        Observation<List<Book>> search = measure(() -> books.search(Genre.TECHNOLOGY, 2010));
        assertThat(search.result()).extracting(Book::title)
                .containsExactly("Clean Architecture", "Effective Java", "The Clean Coder");
        queries.add(bookQuery("search", Map.of("genre", "TECHNOLOGY", "year", 2010), search));
        evidence.put("queries", queries);

        Observation<List<Map<String, String>>> before = measure(() -> authorNames(books.findAll()));
        Observation<List<Map<String, String>>> after = measure(() -> authorNames(books.findAllWithAuthor()));
        assertThat(before.result()).hasSize(8);
        assertThat(after.result()).containsExactlyInAnyOrderElementsOf(before.result());
        assertThat(after.sql()).hasSize(1);
        assertThat(before.sql().size()).isGreaterThan(after.sql().size());
        evidence.put("n_plus_one", Map.of("before", authorQuery("findAll + getAuthor().getName()", before),
                "after", authorQuery("findAllWithAuthor + getAuthor().getName()", after)));

        PageRequest request = PageRequest.of(1, 3, Sort.by(Sort.Direction.DESC, "year"));
        Observation<Page<Book>> page = measure(() -> books.findAll(request));
        assertThat(page.result().getTotalElements()).isEqualTo(8);
        assertThat(page.result().getNumber()).isEqualTo(1);
        assertThat(page.result().getContent()).extracting(Book::title)
                .containsExactly("Clean Code", "Java Concurrency in Practice", "Java Puzzlers");
        assertThat(page.sql()).hasSize(2);
        Observation<Slice<Book>> slice = measure(() -> books.findSliceBy(request));
        assertThat(slice.result().getContent()).extracting(Book::title)
                .containsExactlyElementsOf(titles(page.result().getContent()));
        assertThat(slice.result().hasNext()).isTrue();
        assertThat(slice.sql()).hasSize(1);
        Map<String, Object> pageResult = new LinkedHashMap<>(pageQuery(page));
        pageResult.put("total_elements", page.result().getTotalElements());
        pageResult.put("total_pages", page.result().getTotalPages());
        evidence.put("pagination", Map.of("parameters", Map.of("page", 1, "size", 3, "sort", "year,desc"),
                "page", pageResult, "slice", pageQuery(slice)));

        Observation<MvcResult> api = measure(() -> mvc.perform(get("/api/books")
                        .param("page", "1").param("size", "3").param("sort", "year,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(8))
                .andExpect(jsonPath("$.number").value(1)).andExpect(jsonPath("$.size").value(3))
                .andExpect(jsonPath("$.content[0].title").value("Clean Code")).andReturn());
        assertThat(api.sql()).hasSize(2);
        Observation<MvcResult> invalidSort = measure(() -> mvc.perform(get("/api/books")
                        .param("sort", "nosuchfield"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.detail").value(containsString("nosuchfield"))).andReturn());
        assertThat(invalidSort.sql()).isEmpty();
        evidence.put("api", Map.of("paged", httpQuery("/api/books?page=1&size=3&sort=year,desc", api),
                "invalid_sort", httpQuery("/api/books?sort=nosuchfield", invalidSort)));

        long expectedArchived = seed.stream().filter(book -> book.year() < 2010).count();
        Observation<Integer> bulk = measure(() -> service.archiveOlderThan(2010));
        assertThat(bulk.result()).isEqualTo((int) expectedArchived).isEqualTo(5);
        assertThat(bulk.sql()).hasSize(1);
        entityManager.clear();
        List<Book> reloaded = books.findAll();
        assertThat(reloaded).hasSize(8).allSatisfy(book ->
                assertThat(book.isArchived()).isEqualTo(book.year() < 2010));
        Map<String, Object> bulkResult = new LinkedHashMap<>();
        bulkResult.put("method", "archiveOlderThan");
        bulkResult.put("parameters", Map.of("year", 2010));
        bulkResult.put("affected_rows", bulk.result());
        bulkResult.put("sql_count", bulk.sql().size());
        bulkResult.put("sql", bulk.sql());
        bulkResult.put("verified_rows", reloaded.stream().map(book -> Map.of(
                "title", book.title(), "year", book.year(), "archived", book.isArchived())).toList());
        evidence.put("bulk_update", bulkResult);

        Path defaultDirectory = Path.of(System.getProperty("java.io.tmpdir"), "codex-lab08-20261020", "evidence");
        Path directory = Path.of(System.getProperty("lab08.evidence.dir", defaultDirectory.toString()));
        Files.createDirectories(directory);
        Files.writeString(directory.resolve("actual-measurements.json"),
                JsonMapper.builder().build().writerWithDefaultPrettyPrinter().writeValueAsString(evidence) + "\n",
                StandardCharsets.UTF_8);
    }

    private void emptyCatalogue() {
        books.deleteAll();
        entityManager.flush();
        entityManager.clear();
        authors.deleteAll();
        tags.deleteAll();
        entityManager.flush();
        entityManager.clear();
        assertThat(books.count()).isZero();
        assertThat(authors.count()).isZero();
        assertThat(tags.count()).isZero();
    }

    private <T> Observation<T> measure(Callable<T> action) throws Exception {
        entityManager.flush();
        entityManager.clear();
        SqlCapture.begin();
        T result;
        List<String> sql;
        try {
            result = action.call();
        } finally {
            sql = SqlCapture.finish();
        }
        return new Observation<>(result, sql);
    }

    private Map<String, Object> bookQuery(String method, Map<String, Object> parameters,
                                          Observation<List<Book>> observation) {
        assertThat(observation.sql()).hasSize(1);
        return Map.of("method", method, "parameters", parameters, "returned_rows", observation.result().size(),
                "book_titles", titles(observation.result()), "sql_count", observation.sql().size(), "sql", observation.sql());
    }

    private List<String> titles(List<Book> result) {
        return result.stream().map(Book::title).toList();
    }

    private List<Map<String, String>> authorNames(List<Book> result) {
        return result.stream().map(book -> Map.of("title", book.title(), "author", book.getAuthor().getName())).toList();
    }

    private Map<String, Object> authorQuery(String method, Observation<List<Map<String, String>>> observation) {
        return Map.of("method", method, "returned_rows", observation.result().size(), "books", observation.result(),
                "sql_count", observation.sql().size(), "sql", observation.sql());
    }

    private Map<String, Object> pageQuery(Observation<? extends Slice<Book>> observation) {
        Slice<Book> result = observation.result();
        return Map.of("page_number", result.getNumber(), "page_size", result.getSize(),
                "returned_rows", result.getNumberOfElements(), "book_titles", titles(result.getContent()),
                "has_next", result.hasNext(), "sql_count", observation.sql().size(), "sql", observation.sql());
    }

    private Map<String, Object> httpQuery(String request, Observation<MvcResult> observation) throws Exception {
        var response = observation.result().getResponse();
        return Map.of("request", request, "status", response.getStatus(), "content_type", response.getContentType(),
                "body", response.getContentAsString(StandardCharsets.UTF_8),
                "sql_count", observation.sql().size(), "sql", observation.sql());
    }

    private record Observation<T>(T result, List<String> sql) {
    }
}
