package kz.iitu.springlab01.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Locale;

@RestController
@RequestMapping("/api")
public class HelloController {

    @Value("${app.owner:unknown}")
    private String owner;

    @GetMapping("/hello")
    public Greeting hello(
            @RequestParam(defaultValue = "world") String name
    ) {
        return new Greeting(
                "Hello, " + name + "!",
                owner,
                LocalDateTime.now()
        );
    }

    @GetMapping("/info")
    public Info info() {
        return new Info(
                owner,
                System.getProperty("java.version"),
                Runtime.getRuntime().availableProcessors()
        );
    }

    @GetMapping("/palindrome")
    public PalindromeResult palindrome(@RequestParam String text) {
        String normalized = text
                .replaceAll("\\s+", "")
                .toLowerCase(Locale.ROOT);

        boolean palindrome = normalized.equals(
                new StringBuilder(normalized).reverse().toString()
        );

        return new PalindromeResult(
                text,
                normalized,
                palindrome
        );
    }

    public record Greeting(
            String message,
            String owner,
            LocalDateTime timestamp
    ) {}

    public record Info(
            String owner,
            String javaVersion,
            int cpuCores
    ) {}

    public record PalindromeResult(
            String originalText,
            String normalizedText,
            boolean palindrome
    ) {}
}