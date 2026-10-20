package kz.iitu.springlab.lab6.web;

import java.net.URI;
import java.util.Locale;
import java.util.LinkedHashMap;

import jakarta.servlet.http.HttpServletRequest;
import kz.iitu.springlab.lab6.service.BookNotFoundException;
import kz.iitu.springlab.lab6.service.DuplicateTitleException;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

@RestControllerAdvice(annotations = RestController.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

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

    @ExceptionHandler(DuplicateTitleException.class)
    public ProblemDetail duplicate(DuplicateTitleException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, exception.getMessage());
        problem.setTitle("Duplicate book");
        problem.setType(URI.create("urn:problem:duplicate-book"));
        problem.setProperty("bookTitle", exception.getTitle());
        return problem;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ProblemDetail badQuery(IllegalArgumentException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, exception.getMessage());
        problem.setTitle("Invalid request parameter");
        return problem;
    }

    @ExceptionHandler({DataIntegrityViolationException.class, OptimisticLockingFailureException.class})
    public ProblemDetail conflictingWrite(RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT,
                "The book conflicts with existing data or was changed by another request.");
        problem.setTitle("Conflicting update");
        return problem;
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "Correct the invalid fields and submit the request again.");
        problem.setTitle("Validation failed");
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        problem.setProperty("errors", errors);
        return handleExceptionInternal(exception, problem, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "A request parameter has an invalid value."), headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException exception,
            HttpHeaders headers, HttpStatusCode status, WebRequest request) {
        return handleExceptionInternal(exception, ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                "The JSON body is malformed or contains an unsupported value."), headers, status, request);
    }
}
