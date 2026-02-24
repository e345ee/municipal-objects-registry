package ru.itmo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import ru.itmo.domain.ImportOperation;
import ru.itmo.domain.ImportStatus;
import ru.itmo.dto.ImportOperationDto;
import ru.itmo.repository.ImportOperationRepository;
import ru.itmo.websocket.ChangeAction;
import ru.itmo.websocket.WsEventPublisher;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;

@Service
public class ImportOperationService {

    private final ImportOperationRepository repo;
    private final WsEventPublisher ws;

    public ImportOperationService(ImportOperationRepository repo, WsEventPublisher ws) {
        this.repo = repo;
        this.ws = ws;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation start(String txId, String sourceFilename, String bucketName) {
        ImportOperation op = new ImportOperation();
        op.setTxId(txId);
        op.setStatus(ImportStatus.IN_PROGRESS);
        op.setStartedAt(LocalDateTime.now());
        op.setFinishedAt(null);
        op.setAddedCount(null);
        op.setErrorMessage(null);
        op.setSourceFilename(sourceFilename);
        op.setFileBucket(bucketName);
        op.setFileStagingKey(null);
        op.setFileFinalKey(null);

        op = repo.save(op);
        notifyCreated(op);
        return op;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFilePrepared(Long id, String stagingKey) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.FILE_PREPARED);
        op.setFileStagingKey(stagingKey);
        op.setErrorMessage(null);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFileCommitted(Long id, String finalKey) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.FILE_COMMITTED);
        op.setFileFinalKey(finalKey);
        op.setFileStagingKey(null);
        op.setErrorMessage(null);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markDbCommitted(Long id) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.DB_COMMITTED);
        op.setErrorMessage(null);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long id, int addedCount) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.SUCCESS);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(addedCount);
        op.setErrorMessage(null);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompensated(Long id, String message) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.COMPENSATED);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(null);
        op.setErrorMessage(message);
        op.setFileStagingKey(null);
        op.setFileFinalKey(null);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String message) {
        ImportOperation op = getExisting(id);
        op.setStatus(ImportStatus.FAILED);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(null);
        op.setErrorMessage(message);
        repo.save(op);
        notifyUpdated(op);
    }

    @Transactional(readOnly = true)
    public Page<ImportOperation> list(Pageable pageable) {
        return repo.findAllByOrderByStartedAtDesc(pageable);
    }

    @Transactional(readOnly = true)
    public ImportOperation getRequired(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Import operation id=" + id + " not found"));
    }

    private ImportOperation getExisting(Long id) {
        return repo.findById(id).orElseThrow(() -> new NoSuchElementException("Import operation id=" + id + " not found"));
    }

    private void notifyCreated(ImportOperation op) {
        ImportOperation finalOp = op;
        afterCommit(() -> ws.sendChange("ImportOperation", ChangeAction.CREATED, finalOp.getId(), ImportOperationDto.fromEntity(finalOp)));
    }

    private void notifyUpdated(ImportOperation op) {
        ImportOperation finalOp = op;
        afterCommit(() -> ws.sendChange("ImportOperation", ChangeAction.UPDATED, finalOp.getId(), ImportOperationDto.fromEntity(finalOp)));
    }

    private void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            r.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                r.run();
            }
        });
    }
}
