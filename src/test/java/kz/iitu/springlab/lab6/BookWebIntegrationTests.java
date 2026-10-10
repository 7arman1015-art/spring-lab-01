package kz.iitu.springlab.lab6;

import java.util.Locale;

import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.service.BookService;
import kz.iitu.springlab.lab6.web.BookForm;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.MessageSource;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.validation.BindingResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class BookWebIntegrationTests {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private BookService books;

    @Autowired
    private MessageSource messages;

    @Test
    void newFormContainsGenresAndTheCommonAdviceAttribute() throws Exception {
        mvc.perform(get("/books/new"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeExists("form"))
                .andExpect(model().attribute("genres", Genre.values()))
                .andExpect(model().attribute("libraryName", message("library.name", Locale.ENGLISH)));
    }

    @Test
    void invalidFormPreservesValuesAndDoesNotCreateABook() throws Exception {
        int count = books.findAll().size();
        MvcResult result = mvc.perform(post("/books")
                        .param("title", "")
                        .param("author", "Author2")
                        .param("year", "1449")
                        .param("genre", "")
                        .param("available", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "author", "year", "genre"))
                .andExpect(model().attribute("genres", Genre.values()))
                .andReturn();
        BookForm form = (BookForm) result.getModelAndView().getModel().get("form");
        assertThat(form.getAuthor()).isEqualTo("Author2");
        assertThat(form.getYear()).isEqualTo(1449);
        assertThat(form.isAvailable()).isFalse();
        assertThat(books.findAll()).hasSize(count);
    }

    @Test
    void excessiveTitleAndYearAreRejected() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "x".repeat(121))
                        .param("author", "Valid Author")
                        .param("year", "2101")
                        .param("genre", "SCIENCE"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "title", "year"));
    }

    @Test
    void malformedYearAndUnknownGenreReturnFieldErrors() throws Exception {
        int count = books.findAll().size();
        MvcResult result = mvc.perform(post("/books")
                        .param("title", "Preserved Title")
                        .param("author", "Valid Author")
                        .param("year", "not-a-number")
                        .param("genre", "UNKNOWN"))
                .andExpect(status().isOk())
                .andExpect(view().name("books/form"))
                .andExpect(model().attributeHasFieldErrors("form", "year", "genre"))
                .andExpect(model().attribute("genres", Genre.values()))
                .andReturn();
        BindingResult binding = (BindingResult) result.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "form");
        assertThat(binding.getFieldError("year").getCode()).isEqualTo("typeMismatch");
        assertThat(binding.getFieldError("year").getRejectedValue()).isEqualTo("not-a-number");
        assertThat(binding.getFieldError("genre").getCode()).isEqualTo("typeMismatch");
        assertThat(books.findAll()).hasSize(count);
    }

    @Test
    void validSubmissionUsesPrgAndFlashAndCreatesOneBook() throws Exception {
        int count = books.findAll().size();
        MockHttpSession session = new MockHttpSession();
        MvcResult created = mvc.perform(post("/books").session(session)
                        .param("title", "Boundary Year Book")
                        .param("author", "Valid Author")
                        .param("year", "1450")
                        .param("genre", "HISTORY")
                        .param("_available", "on"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"))
                .andExpect(flash().attributeExists("successMessage"))
                .andReturn();
        Book book = books.findAll().getLast();
        assertThat(book.title()).isEqualTo("Boundary Year Book");
        assertThat(book.year()).isEqualTo(1450);
        assertThat(book.available()).isFalse();
        assertThat(books.findAll()).hasSize(count + 1);

        mvc.perform(get("/books").session(session).flashAttrs(created.getFlashMap()))
                .andExpect(status().isOk())
                .andExpect(view().name("books/list"))
                .andExpect(model().attributeExists("successMessage"));
        mvc.perform(get("/books").session(session)).andExpect(status().isOk());
        assertThat(books.findAll()).hasSize(count + 1);
        mvc.perform(get("/api/books/" + book.id()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Boundary Year Book"))
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    void maximumYearAndMaximumTitleAreAccepted() throws Exception {
        mvc.perform(post("/books")
                        .param("title", "x".repeat(120))
                        .param("author", "Valid Author")
                        .param("year", "2100")
                        .param("genre", "ART")
                        .param("available", "true"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/books"));
        Book book = books.findAll().getLast();
        assertThat(book.title()).hasSize(120);
        assertThat(book.year()).isEqualTo(2100);
        assertThat(book.available()).isTrue();
    }

    @Test
    void languageChoicePersistsForValidationAndFlash() throws Exception {
        MockHttpSession session = new MockHttpSession();
        Locale russian = Locale.forLanguageTag("ru");
        mvc.perform(get("/books/new").param("lang", "ru").session(session))
                .andExpect(status().isOk())
                .andExpect(model().attribute("libraryName", message("library.name", russian)));
        MvcResult invalid = mvc.perform(post("/books").session(session)
                        .param("title", "Название")
                        .param("author", "Автор2")
                        .param("year", "2020")
                        .param("genre", "FICTION"))
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("form", "author"))
                .andReturn();
        BindingResult binding = (BindingResult) invalid.getModelAndView().getModel()
                .get(BindingResult.MODEL_KEY_PREFIX + "form");
        assertThat(binding.getFieldError("author").getDefaultMessage())
                .isEqualTo(message("book.author.nodigits", russian));
        MvcResult created = mvc.perform(post("/books").session(session)
                        .param("title", "Русская книга")
                        .param("author", "Автор")
                        .param("year", "2020")
                        .param("genre", "FICTION"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        Book book = books.findAll().getLast();
        assertThat(created.getFlashMap().get("successMessage"))
                .isEqualTo(messages.getMessage("book.created", new Object[]{book.title(), book.id()}, russian));
        mvc.perform(get("/books/new").param("lang", "en").session(session))
                .andExpect(model().attribute("libraryName", message("library.name", Locale.ENGLISH)));
    }

    @Test
    void pageAndApiNotFoundUseDifferentRepresentationsWithLocalizedDetails() throws Exception {
        long missingId = 999999;
        MockHttpSession session = new MockHttpSession();
        Locale russian = Locale.forLanguageTag("ru");
        String detail = messages.getMessage("error.bookNotFound", new Object[]{missingId}, russian);
        mvc.perform(get("/books/" + missingId).param("lang", "ru").session(session))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error/not-found"))
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(model().attribute("message", detail))
                .andExpect(model().attribute("libraryName", message("library.name", russian)));
        mvc.perform(get("/api/books/" + missingId).session(session))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.type").value("urn:problem:book-not-found"))
                .andExpect(jsonPath("$.title").value(message("error.notFound.title", russian)))
                .andExpect(jsonPath("$.detail").value(detail))
                .andExpect(jsonPath("$.instance").value("/api/books/" + missingId));
    }

    private String message(String code, Locale locale) {
        return messages.getMessage(code, null, locale);
    }
}
