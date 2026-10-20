package kz.iitu.springlab.lab8;

import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.service.BookService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BookApiIntegrationTests {

    @Autowired private MockMvc mvc;
    @Autowired private BookService service;

    @Test
    void createFollowLocationUpdateDeleteAndRepeatedDeleteHaveDeliberateStatuses() throws Exception {
        String body = """
                {"title":"API Lifecycle","author":"API Author","year":2020,"genre":"SCIENCE"}
                """;
        var created = mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andExpect(jsonPath("$.author").value("API Author"))
                .andExpect(jsonPath("$.tags").doesNotExist()).andReturn();
        String location = created.getResponse().getHeader("Location");
        mvc.perform(get(location)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("API Lifecycle"));
        mvc.perform(put(location).contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("2020", "2021")))
                .andExpect(status().isOk()).andExpect(jsonPath("$.year").value(2021));
        mvc.perform(delete(location)).andExpect(status().isNoContent()).andExpect(content().string(""));
        mvc.perform(delete(location)).andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void duplicateAndInvalidRequestsUseProblemDetailsWithoutChangingTheCatalogue() throws Exception {
        String body = """
                {"title":"Duplicate Example","author":"API Author","year":2020,"genre":"SCIENCE"}
                """;
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated());
        int count = service.findAll().size();
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"))
                .andExpect(jsonPath("$.bookTitle").value("Duplicate Example"));
        mvc.perform(post("/api/books").contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("Duplicate Example", "")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.title").exists());
        assertThat(service.findAll()).hasSize(count);
    }

    @Test
    void pageDefaultsFilteringSortingAndBadParametersAreChecked() throws Exception {
        service.create("A Paging Example", "Paging Author", 2022, Genre.SCIENCE, true);
        mvc.perform(get("/api/books")).andExpect(status().isOk())
                .andExpect(jsonPath("$.size").value(5))
                .andExpect(jsonPath("$.totalElements").isNumber());
        mvc.perform(get("/api/books?genre=SCIENCE&size=1&sort=year,desc"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].genre").value("SCIENCE"))
                .andExpect(jsonPath("$.size").value(1));
        mvc.perform(get("/api/books?sort=nosuchfield")).andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
        mvc.perform(get("/api/books?size=101")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/books?genre=UNKNOWN")).andExpect(status().isBadRequest());
    }

    @Test
    void entityVersionIncrementsAndTheMvcPageUsesTheAuthorName() throws Exception {
        Book book = service.create("Versioned Book", "Version Author", 2000, Genre.ART, true);
        long version = book.getVersion();
        service.update(book.id(), "Versioned Book", "Version Author", 2001, Genre.ART, true);
        assertThat(service.findById(book.id()).getVersion()).isGreaterThan(version);
        mvc.perform(get("/books/" + book.id())).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Version Author")));
    }
}
