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

@Service
public class ImportOperationService {

    private final ImportOperationRepository repo;
    private final WsEventPublisher ws;

    public ImportOperationService(ImportOperationRepository repo, WsEventPublisher ws) {
        this.repo = repo;
        this.ws = ws;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ImportOperation start() {
        ImportOperation op = new ImportOperation();
        op.setStatus(ImportStatus.IN_PROGRESS);
        op.setStartedAt(LocalDateTime.now());
        op.setFinishedAt(null);
        op.setAddedCount(null);
        op.setErrorMessage(null);

        op = repo.save(op);

        ImportOperation finalOp = op;
        afterCommit(() ->
                ws.sendChange(
                        "ImportOperation",
                        ChangeAction.CREATED,
                        finalOp.getId(),
                        ImportOperationDto.fromEntity(finalOp)
                )
        );

        return op;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markSuccess(Long id, int addedCount) {
        ImportOperation op = repo.findById(id).orElseThrow();
        op.setStatus(ImportStatus.SUCCESS);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(addedCount);
        op.setErrorMessage(null);

        op = repo.save(op);

        ImportOperation finalOp = op;
        afterCommit(() ->
                ws.sendChange(
                        "ImportOperation",
                        ChangeAction.UPDATED,
                        finalOp.getId(),
                        ImportOperationDto.fromEntity(finalOp)
                )
        );
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(Long id, String message) {
        ImportOperation op = repo.findById(id).orElseThrow();
        op.setStatus(ImportStatus.FAILED);
        op.setFinishedAt(LocalDateTime.now());
        op.setAddedCount(null);
        op.setErrorMessage(message);

        op = repo.save(op);

        ImportOperation finalOp = op;
        afterCommit(() ->
                ws.sendChange(
                        "ImportOperation",
                        ChangeAction.UPDATED,
                        finalOp.getId(),
                        ImportOperationDto.fromEntity(finalOp)
                )
        );
    }

    private void afterCommit(Runnable r) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
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

    @Transactional(readOnly = true)
    public Page<ImportOperation> list(Pageable pageable) {
        return repo.findAllByOrderByStartedAtDesc(pageable);
    }
}
