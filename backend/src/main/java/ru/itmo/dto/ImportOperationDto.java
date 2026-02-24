package ru.itmo.dto;

import ru.itmo.domain.ImportOperation;
import ru.itmo.domain.ImportStatus;

import java.time.LocalDateTime;

public class ImportOperationDto {
    private Long id;
    private String txId;
    private ImportStatus status;
    private Integer addedCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private String errorMessage;
    private String sourceFilename;
    private String downloadUrl;

    public static ImportOperationDto fromEntity(ImportOperation e) {
        ImportOperationDto d = new ImportOperationDto();
        d.id = e.getId();
        d.txId = e.getTxId();
        d.status = e.getStatus();
        d.addedCount = e.getAddedCount();
        d.startedAt = e.getStartedAt();
        d.finishedAt = e.getFinishedAt();
        d.errorMessage = e.getErrorMessage();
        d.sourceFilename = e.getSourceFilename();
        if (e.getStatus() == ImportStatus.SUCCESS && e.getFileFinalKey() != null && !e.getFileFinalKey().isBlank()) {
            d.downloadUrl = "/api/imports/" + e.getId() + "/file";
        }
        return d;
    }

    public Long getId() { return id; }
    public String getTxId() { return txId; }
    public ImportStatus getStatus() { return status; }
    public Integer getAddedCount() { return addedCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public String getErrorMessage() { return errorMessage; }
    public String getSourceFilename() { return sourceFilename; }
    public String getDownloadUrl() { return downloadUrl; }
}
