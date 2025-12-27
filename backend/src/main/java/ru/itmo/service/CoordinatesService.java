package ru.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import ru.itmo.dto.CoordinatesPageDto;
import ru.itmo.specification.CoordinatesSpecifications;
import ru.itmo.exeption.DeletionBlockedException;
import ru.itmo.dto.CityPageDto;
import ru.itmo.domain.Coordinates;
import ru.itmo.websocet.ChangeAction;
import ru.itmo.dto.CoordinatesDto;
import ru.itmo.repository.CoordinatesRepository;
import ru.itmo.websocet.WsEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;

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
        ws.sendChange("Coordinates", ChangeAction.CREATED, dto.getId(), dto);

        return dto;
    }

    @Transactional
    public CoordinatesDto update(Long id, CoordinatesDto dto) {
        Coordinates e = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));

        dto.applyToEntity(e);

        CoordinatesDto updated = CoordinatesDto.fromEntity(e);
        ws.sendChange("Coordinates", ChangeAction.UPDATED, updated.getId(), updated);

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
        ws.sendChange("Coordinates", ChangeAction.DELETED, coordId, null);
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
        ws.sendChange("Coordinates", ChangeAction.DELETED, id, null);
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
}