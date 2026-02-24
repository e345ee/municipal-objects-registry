package ru.itmo.controller;

import io.minio.BucketExistsArgs;
import io.minio.MinioClient;
import jakarta.persistence.EntityManagerFactory;
import org.apache.commons.dbcp2.BasicDataSource;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.cache.L2CacheStatsLoggingSwitch;
import ru.itmo.service.InfraFailureSimulationService;
import ru.itmo.storage.ImportFileStorageService;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/infra")
public class AdminInfraController {

    private final DataSource dataSource;
    private final MinioClient minioClient;
    private final ImportFileStorageService fileStorageService;
    private final L2CacheStatsLoggingSwitch l2LoggingSwitch;
    private final EntityManagerFactory emf;
    private final InfraFailureSimulationService infraFailures;

    public AdminInfraController(DataSource dataSource,
                                MinioClient minioClient,
                                ImportFileStorageService fileStorageService,
                                L2CacheStatsLoggingSwitch l2LoggingSwitch,
                                EntityManagerFactory emf,
                                InfraFailureSimulationService infraFailures) {
        this.dataSource = dataSource;
        this.minioClient = minioClient;
        this.fileStorageService = fileStorageService;
        this.l2LoggingSwitch = l2LoggingSwitch;
        this.emf = emf;
        this.infraFailures = infraFailures;
    }

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("timestamp", OffsetDateTime.now().toString());
        body.put("dbPool", dbPool());
        body.put("cache", cache());
        body.put("postgres", postgresHealth());
        body.put("minio", minioHealth());
        body.put("simulation", simulation());
        return body;
    }

    @PostMapping("/simulated-failure")
    public Map<String, Object> setSimulatedFailure(@RequestParam String target, @RequestParam boolean enabled) {
        String t = target == null ? "" : target.trim().toLowerCase();
        switch (t) {
            case "postgres", "db", "postgresql" -> infraFailures.setPostgresFailureEnabled(enabled);
            case "minio", "s3", "storage" -> infraFailures.setMinioFailureEnabled(enabled);
            default -> throw new IllegalArgumentException("target должен быть postgres или minio");
        }
        return overview();
    }

    private Map<String, Object> simulation() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("postgresFailureEnabled", infraFailures.isPostgresFailureEnabled());
        m.put("minioFailureEnabled", infraFailures.isMinioFailureEnabled());
        return m;
    }

    private Map<String, Object> dbPool() {
        Map<String, Object> m = new LinkedHashMap<>();
        if (dataSource instanceof BasicDataSource bds) {
            int active = bds.getNumActive();
            int idle = bds.getNumIdle();
            int maxTotal = bds.getMaxTotal();
            m.put("active", active);
            m.put("idle", idle);
            m.put("maxTotal", maxTotal);
            m.put("freeCapacity", Math.max(0, maxTotal - active));
            m.put("minIdle", bds.getMinIdle());
            m.put("maxIdle", bds.getMaxIdle());
            m.put("maxWaitMillis", bds.getMaxWaitMillis());
        } else {
            m.put("active", null);
            m.put("idle", null);
            m.put("maxTotal", null);
            m.put("freeCapacity", null);
            m.put("note", "DataSource is not BasicDataSource");
        }
        return m;
    }

    private Map<String, Object> cache() {
        Statistics stats = emf.unwrap(SessionFactory.class).getStatistics();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("l2StatsLoggingEnabled", l2LoggingSwitch.isEnabled());
        m.put("statisticsEnabled", stats.isStatisticsEnabled());
        m.put("hits", stats.getSecondLevelCacheHitCount());
        m.put("misses", stats.getSecondLevelCacheMissCount());
        m.put("puts", stats.getSecondLevelCachePutCount());
        return m;
    }

    private Map<String, Object> postgresHealth() {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean actual = false;
        String error = null;
        try (Connection c = dataSource.getConnection()) {
            try (Statement st = c.createStatement()) {
                st.execute("SELECT 1");
            }
            actual = true;
        } catch (Exception e) {
            error = e.getMessage();
        }
        boolean simulated = infraFailures.isPostgresFailureEnabled();
        m.put("actualAvailable", actual);
        m.put("simulatedFailure", simulated);
        m.put("effectiveAvailable", actual && !simulated);
        if (error != null && !error.isBlank()) {
            m.put("message", shorten(error));
        }
        return m;
    }

    private Map<String, Object> minioHealth() {
        Map<String, Object> m = new LinkedHashMap<>();
        boolean reachable = false;
        Boolean bucketExists = null;
        String error = null;
        try {
            bucketExists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(fileStorageService.getBucketName()).build());
            reachable = true;
        } catch (Exception e) {
            error = e.getMessage();
        }
        boolean simulated = infraFailures.isMinioFailureEnabled();
        m.put("actualAvailable", reachable);
        m.put("bucketExists", bucketExists);
        m.put("bucket", fileStorageService.getBucketName());
        m.put("simulatedFailure", simulated);
        m.put("effectiveAvailable", reachable && !simulated);
        if (error != null && !error.isBlank()) {
            m.put("message", shorten(error));
        }
        return m;
    }

    private String shorten(String s) {
        return s.length() > 300 ? s.substring(0, 300) : s;
    }
}
