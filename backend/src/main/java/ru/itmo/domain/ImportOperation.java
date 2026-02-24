package ru.itmo.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "import_operation")
public class ImportOperation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tx_id", nullable = false, unique = true, length = 64)
    private String txId;

    @Column(name = "started_at", nullable = false)
    private LocalDateTime startedAt = LocalDateTime.now();

    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ImportStatus status;

    @Column(name = "added_count")
    private Integer addedCount;

    @Column(name = "error_message")
    private String errorMessage;

    @Column(name = "source_filename")
    private String sourceFilename;

    @Column(name = "file_bucket")
    private String fileBucket;

    @Column(name = "file_staging_key")
    private String fileStagingKey;

    @Column(name = "file_final_key")
    private String fileFinalKey;

    public Long getId() { return id; }

    public String getTxId() { return txId; }
    public void setTxId(String txId) { this.txId = txId; }

    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }

    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }

    public ImportStatus getStatus() { return status; }
    public void setStatus(ImportStatus status) { this.status = status; }

    public Integer getAddedCount() { return addedCount; }
    public void setAddedCount(Integer addedCount) { this.addedCount = addedCount; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public String getSourceFilename() { return sourceFilename; }
    public void setSourceFilename(String sourceFilename) { this.sourceFilename = sourceFilename; }

    public String getFileBucket() { return fileBucket; }
    public void setFileBucket(String fileBucket) { this.fileBucket = fileBucket; }

    public String getFileStagingKey() { return fileStagingKey; }
    public void setFileStagingKey(String fileStagingKey) { this.fileStagingKey = fileStagingKey; }

    public String getFileFinalKey() { return fileFinalKey; }
    public void setFileFinalKey(String fileFinalKey) { this.fileFinalKey = fileFinalKey; }
}
