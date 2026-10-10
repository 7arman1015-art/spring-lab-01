package kz.iitu.springlab.lab6.web;

import java.util.Locale;

import jakarta.validation.Valid;
import kz.iitu.springlab.lab6.domain.Book;
import kz.iitu.springlab.lab6.domain.Genre;
import kz.iitu.springlab.lab6.service.BookService;
import org.springframework.context.MessageSource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/books")
public class BookPageController {

    private final BookService books;
    private final MessageSource messages;

    public BookPageController(BookService books, MessageSource messages) {
        this.books = books;
        this.messages = messages;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("books", books.findAll());
        return "books/list";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable long id, Model model) {
        model.addAttribute("book", books.findById(id));
        return "books/detail";
    }

    @GetMapping("/new")
    public String newBook(Model model) {
        model.addAttribute("form", new BookForm());
        model.addAttribute("genres", Genre.values());
        return "books/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") BookForm form,
                         BindingResult result, Model model,
                         RedirectAttributes redirectAttributes, Locale locale) {
        if (result.hasErrors()) {
            model.addAttribute("genres", Genre.values());
            return "books/form";
        }
        Book book = books.create(form.getTitle(), form.getAuthor(), form.getYear(),
                form.getGenre(), form.isAvailable());
        redirectAttributes.addFlashAttribute("successMessage",
                messages.getMessage("book.created", new Object[]{book.title(), book.id()}, locale));
        return "redirect:/books";
    }
}
