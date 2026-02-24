package ru.itmo.storage;

import ru.itmo.dto.MinioStoredFileDto;

import java.util.List;

public interface ImportFileStorageService {
    String getBucketName();

    String prepare(String txId, byte[] fileBytes, String originalFilename);

    String commitPrepared(String txId, String stagingKey, String originalFilename);

    void rollbackPreparedQuietly(String stagingKey);

    void rollbackCommittedQuietly(String finalKey);

    StoredImportFile download(String finalKey);

    List<MinioStoredFileDto> listCommittedFiles();
}
