package kz.iitu.springlab.service;

import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;

import kz.iitu.springlab.audit.Audited;
import kz.iitu.springlab.audit.RetryOnFailure;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    // The demo generates every result in memory, so requests must stay bounded.
    public static final int MAX_LIMIT = 1_000;

    private final ObjectProvider<CatalogService> self;
    // Keep at most one unfinished sequence per thread; a new key replaces it.
    // AOP retries finish on that thread, and success removes the sequence.
    private final ThreadLocal<RetryAttempt> retryAttempt = new ThreadLocal<>();

    public CatalogService(ObjectProvider<CatalogService> self) {
        this.self = self;
    }

    public String findById(long id) {
        sleep(50);
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("Catalog limit must be between 1 and " + MAX_LIMIT);
        }
        sleep(300);
        return IntStream.rangeClosed(1, limit)
                .mapToObj(i -> "Item no. " + i)
                .toList();
    }

    @Audited(action = "CATALOG_REMOVE")
    public String remove(long id) {
        if (id <= 0) {
            throw new IllegalArgumentException("Invalid identifier: " + id);
        }
        return "Removed item no. " + id;
    }

    // The two calls through this bypass the proxy; only removeTwice is advised.
    public String removeTwice(long id) {
        String first = this.remove(id);
        String second = this.remove(id + 1);
        return first + "; " + second;
    }

    // The provider resolves this bean's proxy after the bean has been created.
    public String removeTwiceFixed(long id) {
        CatalogService proxy = self.getObject();
        String first = proxy.remove(id);
        String second = proxy.remove(id + 1);
        return first + "; " + second;
    }

    @RetryOnFailure(attempts = 3)
    public String retryDemo(String key) {
        Objects.requireNonNull(key, "Retry key must not be null");
        RetryAttempt previous = retryAttempt.get();
        int invocation = previous != null && previous.key().equals(key)
                ? previous.number() + 1 : 1;
        if (invocation < 3) {
            retryAttempt.set(new RetryAttempt(key, invocation));
            throw new IllegalStateException("Temporary catalog failure " + invocation);
        }
        retryAttempt.remove();
        return "Catalog request " + key + " succeeded on attempt " + invocation;
    }

    private record RetryAttempt(String key, int number) {
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
