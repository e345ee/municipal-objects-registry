package ru.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import org.postgresql.util.PSQLException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.itmo.domain.Coordinates;
import ru.itmo.dto.CityPageDto;
import ru.itmo.dto.CoordinatesDto;
import ru.itmo.dto.CoordinatesPageDto;
import ru.itmo.exception.BusinessRuleViolationException;
import ru.itmo.exception.DeletionBlockedException;
import ru.itmo.repository.CoordinatesRepository;
import ru.itmo.specification.CoordinatesSpecifications;
import ru.itmo.websocket.ChangeAction;
import ru.itmo.websocket.WsEventPublisher;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
public class CoordinatesService {

    private final CoordinatesRepository repo;
    private final WsEventPublisher ws;
    private final PlatformTransactionManager txManager;

    public CoordinatesService(CoordinatesRepository repo,
                              WsEventPublisher ws,
                              PlatformTransactionManager txManager) {
        this.repo = repo;
        this.ws = ws;
        this.txManager = txManager;
    }

    @Transactional(readOnly = true)
    public CityPageDto<CoordinatesDto> page(CoordinatesPageDto rq,
                                            org.springframework.data.domain.Pageable pageable) {
        var spec = CoordinatesSpecifications.byRequest(rq);
        var page = repo.findAll(spec, pageable);
        return CityPageDto.fromPage(page.map(CoordinatesDto::fromEntity));
    }

    private void validateUniquePair(float x, Float y, Long currentIdOrNull) {
        boolean exists = (currentIdOrNull == null)
                ? repo.existsByXAndY(x, y)
                : repo.existsByXAndYAndIdNot(x, y, currentIdOrNull);

        if (exists) {
            throw new BusinessRuleViolationException(
                    "COORDINATES_NOT_UNIQUE",
                    "Пара координат должна быть уникальной: (" + x + ", " + y + ")"
            );
        }
    }


    private <T> T runSerializableWithRetry(Supplier<T> action) {
        int maxAttempts = 3;
        long backoffMs = 30;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                TransactionTemplate tt = new TransactionTemplate(txManager);
                tt.setIsolationLevel(TransactionDefinition.ISOLATION_SERIALIZABLE);
                return tt.execute(status -> action.get());
            } catch (RuntimeException ex) {
                if (isSerializationFailure(ex) && attempt < maxAttempts) {
                    try {
                        Thread.sleep(backoffMs);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                    }
                    backoffMs *= 2;
                    continue;
                }
                throw ex;
            }
        }
        throw new IllegalStateException("Unreachable code");
    }

    private boolean isSerializationFailure(Throwable ex) {
        Throwable t = ex;
        while (t.getCause() != null) t = t.getCause();
        if (t instanceof PSQLException psql) {
            return "40001".equals(psql.getSQLState());
        }
        return false;
    }


    public CoordinatesDto create(CoordinatesDto coordinatesDto) {
        return runSerializableWithRetry(() -> doCreate(coordinatesDto));
    }

    private CoordinatesDto doCreate(CoordinatesDto coordinatesDto) {
        Coordinates coordinates = coordinatesDto.toNewEntity();

        validateUniquePair(coordinates.getX(), coordinates.getY(), null);

        coordinates = repo.save(coordinates);
        repo.flush();

        CoordinatesDto dto = CoordinatesDto.fromEntity(coordinates);
        Long id = dto.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.CREATED, id, dto));
        return dto;
    }

    public CoordinatesDto update(Long id, CoordinatesDto dto) {
        return runSerializableWithRetry(() -> doUpdate(id, dto));
    }

    private CoordinatesDto doUpdate(Long id, CoordinatesDto dto) {
        Coordinates e = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));

        dto.applyToEntity(e);

        validateUniquePair(e.getX(), e.getY(), e.getId());

        Coordinates saved = repo.save(e);
        repo.flush();

        CoordinatesDto updated = CoordinatesDto.fromEntity(saved);
        Long ids = updated.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.UPDATED, ids, updated));
        return updated;
    }

    @Transactional(readOnly = true)
    public CoordinatesDto get(Long id) {
        return CoordinatesDto.fromEntity(
                repo.findById(id)
                        .orElseThrow(() -> new EntityNotFoundException("Coordinates с id=" + id + " не найден"))
        );
    }

    @Transactional(readOnly = true)
    public List<CoordinatesDto> list() {
        return repo.findAll()
                .stream()
                .map(CoordinatesDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long coordId) {
        if (!repo.existsById(coordId)) {
            throw new NoSuchElementException("Coordinates id=" + coordId + " не найдены");
        }

        long usage = repo.countCityUsageByCoordinatesId(coordId);
        if (usage > 0) {
            List<Long> cityIds = repo.findCityIdsByCoordinatesId(coordId);
            throw new DeletionBlockedException("Coordinates", coordId, usage, cityIds);
        }

        repo.deleteById(coordId);
        Long id = coordId;
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.DELETED, id, null));
    }

    @Transactional(readOnly = true)
    public Coordinates getEntity(Long id) {
        return repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Coordinates saveEntity(Coordinates coordinates) {
        validateUniquePair(coordinates.getX(), coordinates.getY(), coordinates.getId());
        Coordinates saved = repo.save(coordinates);
        repo.flush();
        return saved;
    }

    @Transactional
    public void deleteEntity(Long id) {
        repo.deleteById(id);
        Long cid = id;
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.DELETED, cid, null));
    }

    @Transactional(readOnly = true)
    public Optional<Coordinates> findById(Long id) {
        return repo.findById(id);
    }

    @Transactional(isolation = Isolation.SERIALIZABLE)
    public Coordinates save(Coordinates coordinates) {
        validateUniquePair(coordinates.getX(), coordinates.getY(), coordinates.getId());
        Coordinates saved = repo.save(coordinates);
        repo.flush();
        return saved;
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    private void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    try {
                        r.run();
                    } catch (Exception ignored) {
                    }
                }
            });
        } else {
            r.run();
        }
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Coordinates saveNewAndNotify(Coordinates coordinates) {
        validateUniquePair(coordinates.getX(), coordinates.getY(), null);

        Coordinates saved = repo.save(coordinates);
        repo.flush();

        CoordinatesDto dto = CoordinatesDto.fromEntity(saved);
        Long id = dto.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.CREATED, id, dto));
        return saved;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Coordinates saveUpdatedAndNotify(Coordinates coordinates) {
        validateUniquePair(coordinates.getX(), coordinates.getY(), coordinates.getId());

        Coordinates saved = repo.save(coordinates);
        repo.flush();

        CoordinatesDto dto = CoordinatesDto.fromEntity(saved);
        Long id = dto.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.UPDATED, id, dto));
        return saved;
    }

    @Transactional
    public void deleteByIdAndNotify(Long id) {
        repo.deleteById(id);
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.DELETED, id, null));
    }
}
