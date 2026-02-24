package ru.itmo.cache;

import jakarta.persistence.EntityManagerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class L2CacheStatsAspect {

    private static final Logger log = LoggerFactory.getLogger(L2CacheStatsAspect.class);

    private final EntityManagerFactory emf;
    private final L2CacheStatsLoggingSwitch loggingSwitch;

    public L2CacheStatsAspect(EntityManagerFactory emf, L2CacheStatsLoggingSwitch loggingSwitch) {
        this.emf = emf;
        this.loggingSwitch = loggingSwitch;
    }

    @Around("execution(public * ru.itmo.service..*(..))")
    public Object logL2Stats(ProceedingJoinPoint pjp) throws Throwable {
        SessionFactory sessionFactory = emf.unwrap(SessionFactory.class);
        Statistics stats = sessionFactory.getStatistics();

        boolean enabled = loggingSwitch.isEnabled();
        stats.setStatisticsEnabled(enabled);

        if (!enabled) {
            return pjp.proceed();
        }

        long hitsBefore = stats.getSecondLevelCacheHitCount();
        long missesBefore = stats.getSecondLevelCacheMissCount();
        long putsBefore = stats.getSecondLevelCachePutCount();

        try {
            return pjp.proceed();
        } finally {
            long hitsDelta = stats.getSecondLevelCacheHitCount() - hitsBefore;
            long missesDelta = stats.getSecondLevelCacheMissCount() - missesBefore;
            long putsDelta = stats.getSecondLevelCachePutCount() - putsBefore;

            log.info("[L2-cache] {} -> hits:+{}, misses:+{}, puts:+{}",
                    pjp.getSignature().toShortString(), hitsDelta, missesDelta, putsDelta);
        }
    }
}
