package kz.iitu.springlab.audit;

import java.time.Instant;
import java.util.Arrays;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(1)
public class AuditAspect {

    private static final Logger log = LoggerFactory.getLogger(AuditAspect.class);

    @Around(value = "@annotation(audited)", argNames = "audited")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        Instant started = Instant.now();
        if (audited.logArguments()) {
            log.info("[AUDIT] start {} at={} args={}", audited.action(), started,
                    Arrays.toString(pjp.getArgs()));
        } else {
            log.info("[AUDIT] start {} at={}", audited.action(), started);
        }
        try {
            Object result = pjp.proceed();
            log.info("[AUDIT] {} success at={}", audited.action(), Instant.now());
            return result;
        } catch (Throwable ex) {
            log.warn("[AUDIT] {} failure at={} error={}", audited.action(),
                    Instant.now(), ex.getMessage());
            throw ex;
        }
    }
}
