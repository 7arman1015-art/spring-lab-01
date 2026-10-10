package kz.iitu.springlab.lab6.web;

import java.net.URI;
import java.util.Locale;

import jakarta.servlet.http.HttpServletRequest;
import kz.iitu.springlab.lab6.service.BookNotFoundException;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler {

    private final MessageSource messages;

    public ApiExceptionHandler(MessageSource messages) {
        this.messages = messages;
    }

    @ExceptionHandler(BookNotFoundException.class)
    public ProblemDetail bookNotFound(BookNotFoundException exception, Locale locale,
                                     HttpServletRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                messages.getMessage("error.bookNotFound", new Object[]{exception.getBookId()}, locale));
        problem.setTitle(messages.getMessage("error.notFound.title", null, locale));
        problem.setType(URI.create("urn:problem:book-not-found"));
        problem.setInstance(URI.create(request.getRequestURI()));
        return problem;
    }
}
