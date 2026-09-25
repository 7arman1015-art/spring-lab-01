package kz.iitu.springlab.service;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.IntStream;

import kz.iitu.springlab.audit.Audited;
import kz.iitu.springlab.audit.RetryOnFailure;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

@Service
public class CatalogService {

    private final ObjectProvider<CatalogService> self;
    private final ConcurrentHashMap<String, AtomicInteger> retryCounts =
            new ConcurrentHashMap<>();

    public CatalogService(ObjectProvider<CatalogService> self) {
        this.self = self;
    }

    public String findById(long id) {
        sleep(50);
        return "Item no. " + id;
    }

    @Audited(action = "CATALOG_LIST", logArguments = true)
    public List<String> findAll(int limit) {
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
        int invocation = retryCounts
                .computeIfAbsent(key, ignored -> new AtomicInteger())
                .incrementAndGet();
        if (invocation < 3) {
            throw new IllegalStateException("Temporary catalog failure " + invocation);
        }
        return "Catalog request " + key + " succeeded on attempt " + invocation;
    }

    private void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
