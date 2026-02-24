package ru.itmo.cache;

import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/cache")
public class CacheAdminController {

    private final L2CacheStatsLoggingSwitch loggingSwitch;
    private final EntityManagerFactory emf;

    public CacheAdminController(L2CacheStatsLoggingSwitch loggingSwitch, EntityManagerFactory emf) {
        this.loggingSwitch = loggingSwitch;
        this.emf = emf;
    }

    @PostMapping("/l2-stats-logging")
    public Map<String, Object> setLogging(@RequestParam boolean enabled) {
        loggingSwitch.setEnabled(enabled);

        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        stats.setStatisticsEnabled(enabled);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("l2StatsLoggingEnabled", enabled);
        response.put("hibernateStatisticsEnabled", stats.isStatisticsEnabled());
        return response;
    }

    @GetMapping("/l2-stats")
    public Map<String, Object> getStats() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("l2StatsLoggingEnabled", loggingSwitch.isEnabled());
        response.put("statisticsEnabled", stats.isStatisticsEnabled());
        response.put("hits", stats.getSecondLevelCacheHitCount());
        response.put("misses", stats.getSecondLevelCacheMissCount());
        response.put("puts", stats.getSecondLevelCachePutCount());
        return response;
    }

    @PostMapping("/l2-stats/reset")
    public Map<String, String> resetStats() {
        emf.unwrap(SessionFactory.class).getStatistics().clear();

        Map<String, String> response = new LinkedHashMap<>();
        response.put("status", "ok");
        return response;
    }
}
