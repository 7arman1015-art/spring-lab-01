package kz.iitu.springlab.lab6.web;

import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import kz.iitu.springlab.lab6.service.BookNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.ResponseStatus;

@ControllerAdvice(assignableTypes = BookPageController.class)
public class PageExceptionHandler {

    private final MessageSource messages;

    public PageExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    // Individual variant 11: one common model attribute for all HTML pages.
    @ModelAttribute("libraryName")
    public String libraryName(Locale locale) {
        return messages.getMessage("library.name", null, locale);
    }

    @ExceptionHandler(BookNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String bookNotFound(BookNotFoundException exception, Model model,
                               Locale locale, HttpServletRequest request) {
        model.addAttribute("libraryName", libraryName(locale));
        model.addAttribute("title", messages.getMessage("error.notFound.title", null, locale));
        model.addAttribute("message", messages.getMessage("error.bookNotFound",
                new Object[]{exception.getBookId()}, locale));
        model.addAttribute("requestedPath", request.getRequestURI());
        return "error/not-found";
    }
}
