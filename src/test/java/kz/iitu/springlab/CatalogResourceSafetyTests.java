package kz.iitu.springlab;

import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import kz.iitu.springlab.aspect.RetryOnFailureAspect;
import kz.iitu.springlab.service.CatalogService;
import kz.iitu.springlab.web.CatalogController;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class CatalogResourceSafetyTests {

    private final CatalogService service = new CatalogService(
            new StaticListableBeanFactory().getBeanProvider(CatalogService.class));

    @ParameterizedTest
    @ValueSource(ints = {-1, 0, 1001})
    void directCallsRejectLimitsOutsideTheBound(int limit) {
        assertThatIllegalArgumentException().isThrownBy(() -> service.findAll(limit))
                .withMessage("Catalog limit must be between 1 and 1000");
    }

    @Test
    void maximumLimitRemainsUsable() {
        assertThat(service.findAll(CatalogService.MAX_LIMIT))
                .hasSize(1000)
                .startsWith("Item no. 1")
                .endsWith("Item no. 1000");
    }

    @Test
    void endpointRejectsOversizedRequestsAndPreservesTheDefault() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(new CatalogController(service)).build();
        mvc.perform(get("/api/lab4/items").param("limit", "1001"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/lab4/items").param("limit", "0"))
                .andExpect(status().isBadRequest());
        mvc.perform(get("/api/lab4/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(5))
                .andExpect(jsonPath("$[0]").value("Item no. 1"));
    }

    @Test
    void completedRetriesClearStateForTheNextCall() {
        // Reusing a completed key must start again at attempt 1, not retain it.
        for (int request = 0; request < 50; request++) {
            completeThreeAttempts("reused-key");
        }
    }

    @Test
    void aDifferentDirectCallReplacesAnUnfinishedSequence() {
        assertThatIllegalStateException().isThrownBy(() -> service.retryDemo("unfinished-key"))
                .withMessage("Temporary catalog failure 1");
        completeThreeAttempts("new-key");
    }

    @Test
    void aopStillCompletesEveryRequestOnTheThirdAttempt() {
        AspectJProxyFactory factory = new AspectJProxyFactory(service);
        factory.addAspect(new RetryOnFailureAspect());
        CatalogService proxy = factory.getProxy();
        for (int request = 0; request < 50; request++) {
            String key = "request-" + request;
            assertThat(proxy.retryDemo(key))
                    .isEqualTo("Catalog request " + key + " succeeded on attempt 3");
        }
        // A completed AOP invocation also leaves the direct-call state empty.
        completeThreeAttempts("request-49");
    }

    @Test
    void simultaneousRequestsWithTheSameKeyDoNotShareAttempts() throws Exception {
        CyclicBarrier barrier = new CyclicBarrier(2);
        try (var executor = Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> completeConcurrentAttempts(barrier));
            var second = executor.submit(() -> completeConcurrentAttempts(barrier));
            assertThat(first.get(10, TimeUnit.SECONDS)).isEqualTo(
                    "Catalog request concurrent-key succeeded on attempt 3");
            assertThat(second.get(10, TimeUnit.SECONDS)).isEqualTo(
                    "Catalog request concurrent-key succeeded on attempt 3");
        }
    }

    private void completeThreeAttempts(String key) {
        assertThatIllegalStateException().isThrownBy(() -> service.retryDemo(key))
                .withMessage("Temporary catalog failure 1");
        assertThatIllegalStateException().isThrownBy(() -> service.retryDemo(key))
                .withMessage("Temporary catalog failure 2");
        assertThat(service.retryDemo(key))
                .isEqualTo("Catalog request " + key + " succeeded on attempt 3");
    }

    private String completeConcurrentAttempts(CyclicBarrier barrier) throws Exception {
        assertThatIllegalStateException().isThrownBy(() -> service.retryDemo("concurrent-key"))
                .withMessage("Temporary catalog failure 1");
        barrier.await(5, TimeUnit.SECONDS);
        assertThatIllegalStateException().isThrownBy(() -> service.retryDemo("concurrent-key"))
                .withMessage("Temporary catalog failure 2");
        barrier.await(5, TimeUnit.SECONDS);
        String result = service.retryDemo("concurrent-key");
        // Verify cleanup on the same worker thread, then finish that sequence too.
        completeThreeAttempts("concurrent-key");
        return result;
    }
}
