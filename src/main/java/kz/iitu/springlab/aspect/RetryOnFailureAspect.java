package kz.iitu.springlab.aspect;

import kz.iitu.springlab.audit.RetryOnFailure;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(4)
public class RetryOnFailureAspect {

    private static final Logger log = LoggerFactory.getLogger(RetryOnFailureAspect.class);
    private static final String RETRY_OPERATION =
            "kz.iitu.springlab.aspect.Pointcuts.retryOperation(retry)";

    @Around(value = RETRY_OPERATION, argNames = "retry")
    public Object retry(ProceedingJoinPoint pjp, RetryOnFailure retry) throws Throwable {
        int maxAttempts = retry.attempts();
        if (maxAttempts < 1) {
            throw new IllegalArgumentException("attempts must be positive");
        }
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                log.info("[RETRY] {} attempt {}/{}", pjp.getSignature().toShortString(),
                        attempt, maxAttempts);
                return pjp.proceed();
            } catch (Exception ex) {
                if (attempt == maxAttempts) {
                    log.warn("[RETRY] exhausted {} attempts: {}", maxAttempts, ex.getMessage());
                    throw ex;
                }
                log.warn("[RETRY] attempt {} failed: {}", attempt, ex.getMessage());
            }
        }
        throw new IllegalStateException("The retry loop did not execute");
    }

    // Demonstrates the fifth advice type: @After runs for both outcomes.
    @After(value = RETRY_OPERATION, argNames = "retry")
    public void after(JoinPoint jp, RetryOnFailure retry) {
        log.info("[RETRY] completed {} (maximum {} attempts)",
                jp.getSignature().toShortString(), retry.attempts());
    }
}
