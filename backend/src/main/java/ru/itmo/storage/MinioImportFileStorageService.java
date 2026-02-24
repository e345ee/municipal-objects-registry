package ru.itmo.storage;

import io.minio.BucketExistsArgs;
import io.minio.CopyObjectArgs;
import io.minio.CopySource;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.ListObjectsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.Result;
import io.minio.messages.Item;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ru.itmo.dto.MinioStoredFileDto;
import ru.itmo.service.InfraFailureSimulationService;
import ru.itmo.websocket.ChangeAction;
import ru.itmo.websocket.WsEventPublisher;

import java.io.ByteArrayInputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class MinioImportFileStorageService implements ImportFileStorageService {

    private static final Logger log = LoggerFactory.getLogger(MinioImportFileStorageService.class);
    private static final String COMMITTED_PREFIX = "imports/committed/";

    private final MinioClient minioClient;
    private final String bucketName;
    private final boolean autoCreateBucket;
    private final WsEventPublisher ws;
    private final InfraFailureSimulationService infraFailures;

    public MinioImportFileStorageService(MinioClient minioClient,
                                         @Value("${minio.bucket:${MINIO_BUCKET:avatars}}") String bucketName,
                                         @Value("${minio.autoCreateBucket:${MINIO_AUTO_CREATE_BUCKET:true}}") boolean autoCreateBucket,
                                         WsEventPublisher ws,
                                         InfraFailureSimulationService infraFailures) {
        this.minioClient = minioClient;
        this.bucketName = bucketName;
        this.autoCreateBucket = autoCreateBucket;
        this.ws = ws;
        this.infraFailures = infraFailures;
    }

    @PostConstruct
    public void ensureBucket() {
        try {
            boolean exists = minioClient.bucketExists(BucketExistsArgs.builder().bucket(bucketName).build());
            if (!exists) {
                if (!autoCreateBucket) {
                    throw new IllegalStateException("MinIO bucket does not exist: " + bucketName);
                }
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucketName).build());
                log.info("Created MinIO bucket {}", bucketName);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to initialize MinIO bucket " + bucketName + ": " + e.getMessage(), e);
        }
    }

    @Override
    public String getBucketName() {
        return bucketName;
    }

    @Override
    public String prepare(String txId, byte[] fileBytes, String originalFilename) {
        infraFailures.assertMinioAvailable();
        String objectKey = "imports/staging/" + txId + "/" + normalizeFilename(originalFilename, txId);
        try {
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectKey)
                    .contentType("application/json")
                    .stream(new ByteArrayInputStream(fileBytes), fileBytes.length, -1)
                    .build());
            return objectKey;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to prepare import file in MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public String commitPrepared(String txId, String stagingKey, String originalFilename) {
        infraFailures.assertMinioAvailable();
        String finalKey = COMMITTED_PREFIX + txId + "/" + normalizeFilename(originalFilename, txId);
        try {
            minioClient.copyObject(CopyObjectArgs.builder()
                    .bucket(bucketName)
                    .object(finalKey)
                    .source(CopySource.builder().bucket(bucketName).object(stagingKey).build())
                    .build());
        } catch (Exception e) {
            throw new IllegalStateException("Failed to commit prepared import file in MinIO: " + e.getMessage(), e);
        }

        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(stagingKey).build());
        } catch (Exception e) {
            log.warn("Prepared object cleanup failed after commit (stagingKey={}): {}", stagingKey, e.getMessage());
        }

        ws.sendChange("MinioFile", ChangeAction.CREATED, null, null);
        return finalKey;
    }

    @Override
    public void rollbackPreparedQuietly(String stagingKey) {
        infraFailures.assertMinioAvailable();
        removeQuietly(stagingKey, "prepared", false);
    }

    @Override
    public void rollbackCommittedQuietly(String finalKey) {
        infraFailures.assertMinioAvailable();
        removeQuietly(finalKey, "committed", true);
    }

    @Override
    public StoredImportFile download(String objectKey) {
        infraFailures.assertMinioAvailable();
        try (GetObjectResponse response = minioClient.getObject(GetObjectArgs.builder()
                .bucket(bucketName)
                .object(objectKey)
                .build())) {
            String contentType = response.headers() != null ? response.headers().get("Content-Type") : null;
            return new StoredImportFile(response.readAllBytes(), contentType);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to download import file from MinIO: " + e.getMessage(), e);
        }
    }

    @Override
    public List<MinioStoredFileDto> listCommittedFiles() {
        infraFailures.assertMinioAvailable();

        List<Row> rows = new ArrayList<>();
        try {
            Iterable<Result<Item>> iterable = minioClient.listObjects(ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .prefix(COMMITTED_PREFIX)
                    .recursive(true)
                    .build());

            for (Result<Item> result : iterable) {
                Item item = result.get();
                if (item.isDir()) {
                    continue;
                }
                var zdt = item.lastModified();
                OffsetDateTime lm = zdt != null ? zdt.toOffsetDateTime() : null;
                String key = item.objectName();
                rows.add(new Row(key, fileNameFromKey(key), item.size(), lm, lm != null ? lm.toString() : null));
            }
        } catch (Exception e) {
            throw new IllegalStateException("Failed to list MinIO files: " + e.getMessage(), e);
        }

        rows.sort(Comparator
                .comparing((Row r) -> r.lastModified == null ? OffsetDateTime.MIN : r.lastModified)
                .reversed()
                .thenComparing(r -> r.objectKey, Comparator.reverseOrder()));

        List<MinioStoredFileDto> out = new ArrayList<>(rows.size());
        for (Row r : rows) {
            MinioStoredFileDto dto = new MinioStoredFileDto(r.objectKey, r.fileName, r.sizeBytes, r.lastModifiedText);
            dto.setDownloadUrl("/api/admin/storage/file?key=" + URLEncoder.encode(r.objectKey, StandardCharsets.UTF_8));
            out.add(dto);
        }
        return out;
    }

    private void removeQuietly(String objectKey, String phase, boolean notifyFileList) {
        if (objectKey == null || objectKey.isBlank()) return;
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectKey).build());
            if (notifyFileList) {
                ws.sendChange("MinioFile", ChangeAction.DELETED, null, null);
            }
        } catch (Exception e) {
            log.warn("Failed to rollback {} object {} in bucket {}: {}", phase, objectKey, bucketName, e.getMessage());
            throw new IllegalStateException("Failed to rollback " + phase + " file in MinIO: " + e.getMessage(), e);
        }
    }

    private String normalizeFilename(String originalFilename, String txId) {
        String base = (originalFilename == null || originalFilename.isBlank())
                ? ("import-" + txId + ".json")
                : originalFilename;

        String safe = base.replace('\\', '_').replace('/', '_').trim();
        if (safe.isBlank()) {
            safe = "import-" + txId + ".json";
        }

        safe = safe.replaceAll("[^a-zA-Z0-9._-]", "_");
        if (!safe.toLowerCase(Locale.ROOT).endsWith(".json")) {
            safe = safe + ".json";
        }
        return safe;
    }

    private String fileNameFromKey(String key) {
        if (key == null || key.isBlank()) return "file.json";
        int idx = key.lastIndexOf('/');
        return idx >= 0 ? key.substring(idx + 1) : key;
    }

    private static final class Row {
        private final String objectKey;
        private final String fileName;
        private final long sizeBytes;
        private final OffsetDateTime lastModified;
        private final String lastModifiedText;

        private Row(String objectKey, String fileName, long sizeBytes, OffsetDateTime lastModified, String lastModifiedText) {
            this.objectKey = objectKey;
            this.fileName = fileName;
            this.sizeBytes = sizeBytes;
            this.lastModified = lastModified;
            this.lastModifiedText = lastModifiedText;
        }
    }
}
