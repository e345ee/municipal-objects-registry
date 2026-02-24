package ru.itmo.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

@Configuration
@PropertySource("classpath:db.properties")
public class MinioConfig {

    private final Environment env;

    public MinioConfig(Environment env) {
        this.env = env;
    }

    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(required("minio.endpoint", "MINIO_ENDPOINT"))
                .credentials(required("minio.accessKey", "MINIO_ACCESS_KEY"), required("minio.secretKey", "MINIO_SECRET_KEY"))
                .build();
    }

    private String required(String propertyKey, String envKey) {
        String value = env.getProperty(propertyKey);
        if (value == null || value.isBlank()) {
            value = env.getProperty(envKey);
        }
        if (value == null || value.isBlank()) {
            throw new IllegalStateException("Missing required property: " + propertyKey + " (or env " + envKey + ")");
        }
        return value.trim();
    }
}
