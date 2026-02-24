package ru.itmo.service;

import org.springframework.stereotype.Service;

import java.util.concurrent.atomic.AtomicBoolean;

@Service
public class InfraFailureSimulationService {

    private final AtomicBoolean minioFailureEnabled = new AtomicBoolean(false);
    private final AtomicBoolean postgresFailureEnabled = new AtomicBoolean(false);

    public boolean isMinioFailureEnabled() {
        return minioFailureEnabled.get();
    }

    public boolean isPostgresFailureEnabled() {
        return postgresFailureEnabled.get();
    }

    public void setMinioFailureEnabled(boolean enabled) {
        minioFailureEnabled.set(enabled);
    }

    public void setPostgresFailureEnabled(boolean enabled) {
        postgresFailureEnabled.set(enabled);
    }

    public void assertMinioAvailable() {
        if (isMinioFailureEnabled()) {
            throw new IllegalStateException("MinIO unavailable (simulated failure)");
        }
    }

    public void assertPostgresAvailable() {
        if (isPostgresFailureEnabled()) {
            throw new IllegalStateException("PostgreSQL unavailable (simulated failure)");
        }
    }
}
