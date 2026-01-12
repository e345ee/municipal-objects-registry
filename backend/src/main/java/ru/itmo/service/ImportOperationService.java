package ru.itmo.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import ru.itmo.domain.ImportOperation;
import ru.itmo.domain.ImportStatus;
import ru.itmo.repository.ImportOperationRepository;

import java.time.LocalDateTime;

@Service
public class ImportOperationService {

    private final ImportOperationRepository repo;

    public ImportOperationService(ImportOperationRepository repo) {
        this.repo = repo;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation start() {
        ImportOperation op = new ImportOperation();
        op.setStatus(ImportStatus.IN_PROGRESS);
        op.setStartedAt(LocalDateTime.now());
        return repo.save(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long id, int addedCount) {
        ImportOperation op = repo.findById(id).orElseThrow();
        op.setStatus(ImportStatus.SUCCESS);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(addedCount);
        op.setErrorMessage(null);
        repo.save(op);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String message) {
        ImportOperation op = repo.findById(id).orElseThrow();
        op.setStatus(ImportStatus.FAILED);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(null);
        op.setErrorMessage(message);
        repo.save(op);
    }

    @Transactional(readOnly = true)
    public Page<ImportOperation> list(Pageable pageable) {
        return repo.findAllByOrderByStartedAtDesc(pageable);
    }
}
