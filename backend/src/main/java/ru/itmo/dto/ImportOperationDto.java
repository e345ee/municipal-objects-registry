package ru.itmo.dto;

import ru.itmo.domain.ImportOperation;
import ru.itmo.domain.ImportStatus;

import java.time.LocalDateTime;

public class ImportOperationDto {
    private Long id;
    private ImportStatus status;
    private Integer addedCount;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public static ImportOperationDto fromEntity(ImportOperation e) {
        ImportOperationDto d = new ImportOperationDto();
        d.id = e.getId();
        d.status = e.getStatus();
        d.addedCount = e.getAddedCount();
        d.startedAt = e.getStartedAt();
        d.finishedAt = e.getFinishedAt();
        return d;
    }

    public Long getId() { return id; }
    public ImportStatus getStatus() { return status; }
    public Integer getAddedCount() { return addedCount; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
}
