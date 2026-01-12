package ru.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import ru.itmo.dto.CoordinatesPageDto;
import ru.itmo.specification.CoordinatesSpecifications;
import ru.itmo.exception.DeletionBlockedException;
import ru.itmo.dto.CityPageDto;
import ru.itmo.domain.Coordinates;
import ru.itmo.websocket.ChangeAction;
import ru.itmo.dto.CoordinatesDto;
import ru.itmo.repository.CoordinatesRepository;
import ru.itmo.websocket.WsEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;


import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CoordinatesService {

    private final CoordinatesRepository repo;
    private final WsEventPublisher ws;

    public CoordinatesService(CoordinatesRepository repo,
                              WsEventPublisher ws) {
        this.repo = repo;
        this.ws = ws;
    }

    @Transactional(readOnly = true)
    public CityPageDto<CoordinatesDto> page(CoordinatesPageDto rq,
                                            org.springframework.data.domain.Pageable pageable) {
        var spec = CoordinatesSpecifications.byRequest(rq);
        Page<Coordinates> page = repo.findAll(spec, pageable);
        return CityPageDto.fromPage(page.map(CoordinatesDto::fromEntity));
    }

    @Transactional
    public CoordinatesDto create(CoordinatesDto coordinatesDto) {
        Coordinates coordinates = coordinatesDto.toNewEntity();

        coordinates = repo.save(coordinates);

        CoordinatesDto dto = CoordinatesDto.fromEntity(coordinates);
        Long id = dto.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.CREATED, id, dto));
        return dto;
    }

    @Transactional
    public CoordinatesDto update(Long id, CoordinatesDto dto) {
        Coordinates e = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));

        dto.applyToEntity(e);

        CoordinatesDto updated = CoordinatesDto.fromEntity(e);
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

    @Transactional
    public Coordinates saveEntity(Coordinates coordinates) {
        return repo.save(coordinates);
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

    @Transactional
    public Coordinates save(Coordinates coordinates) {
        return repo.save(coordinates);
    }

    @Transactional
    public void deleteById(Long id) {
        repo.deleteById(id);
    }

    private void afterCommit(Runnable r) {
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override public void afterCommit() { try { r.run(); } catch (Exception ignored) {} }
            });
        } else {
            r.run();
        }
    }

    @Transactional
    public Coordinates saveNewAndNotify(Coordinates coordinates) {
        Coordinates saved = repo.save(coordinates);
        CoordinatesDto dto = CoordinatesDto.fromEntity(saved);
        Long id = dto.getId();
        afterCommit(() -> ws.sendChange("Coordinates", ChangeAction.CREATED, id, dto));
        return saved;
    }

    @Transactional
    public Coordinates saveUpdatedAndNotify(Coordinates coordinates) {
        Coordinates saved = repo.save(coordinates);
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