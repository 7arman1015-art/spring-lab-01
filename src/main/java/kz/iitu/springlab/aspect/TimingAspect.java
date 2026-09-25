package kz.iitu.springlab.aspect;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(3)
public class TimingAspect {

    private static final Logger log = LoggerFactory.getLogger(TimingAspect.class);

    @Around("kz.iitu.springlab.aspect.Pointcuts.serviceOperation()")
    public Object measure(ProceedingJoinPoint pjp) throws Throwable {
        long started = System.nanoTime();
        try {
            return pjp.proceed();
        } finally {
            long elapsedMs = (System.nanoTime() - started) / 1_000_000;
            String method = pjp.getSignature().toShortString();
            if (elapsedMs > 200) {
                log.warn("[TIME] SLOW: {} — {} ms", method, elapsedMs);
            } else {
                log.info("[TIME] {} — {} ms", method, elapsedMs);
            }
        }
    }
}
