package org.itmo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.itmo.api.*;
import org.itmo.domain.City;
import org.itmo.domain.Coordinates;
import org.itmo.dto.ChangeAction;
import org.itmo.dto.CityDto;
import org.itmo.dto.CoordinatesDto;
import org.itmo.repository.CityRepository;
import org.itmo.repository.CoordinatesRepository;
import org.itmo.websocet.WsEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Service
public class CoordinatesService {

    private  final CoordinatesRepository repo;
    private final CityRepository cityRepo;
    private final WsEventPublisher ws;

    @PersistenceContext
    private EntityManager em;

    public CoordinatesService(CoordinatesRepository repo,
                              CityRepository cityRepo,
                              WsEventPublisher ws) {
        this.repo = repo;
        this.cityRepo = cityRepo;
        this.ws = ws;
    }

    @Transactional(readOnly = true)
    public PageDto<CoordinatesDto> page(CoordinatesPageRequest rq,
                                        org.springframework.data.domain.Pageable pageable) {
        var spec = CoordinatesSpecifications.byRequest(rq);
        Page<Coordinates> page = repo.findAll(spec, pageable);
        return PageDto.fromPage(page.map(CoordinatesDto::fromEntity));
    }

    @Transactional
    public CoordinatesDto create(CoordinatesDto coordinatesDto) {
        Coordinates coordinates = coordinatesDto.toNewEntity();

        coordinates = repo.save(coordinates);
        repo.flush();
        em.refresh(coordinates);

        ws.sendChange("Coordinates", ChangeAction.CREATED, CoordinatesDto.fromEntity(coordinates).getId(), CoordinatesDto.fromEntity(coordinates));
        return CoordinatesDto.fromEntity(coordinates);
    }


    @Transactional
    public CoordinatesDto update(Long id, CoordinatesDto dto) {
        Coordinates e = repo.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));

        dto.applyToEntity(e);

        ws.sendChange("Coordinates", ChangeAction.UPDATED, CoordinatesDto.fromEntity(e).getId(), CoordinatesDto.fromEntity(e));

        return CoordinatesDto.fromEntity(e);
    }

    @Transactional(readOnly = true)
    public CoordinatesDto get(Long id) {
        return CoordinatesDto.fromEntity(repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Coordinates с id=" + id + " не найден")));
    }


    @Transactional(readOnly = true)
    public List<CoordinatesDto> list(){
        return repo.findAll().stream().map(CoordinatesDto::fromEntity).collect(Collectors.toList());
    }

    @Transactional
    public void delete(Long coordId) {
        if (!repo.existsById(coordId)) {
            throw new NoSuchElementException("Coordinates id=" + coordId + " не найдены");
        }
        long usage = cityRepo.countByCoordinates_Id(coordId);
        if (usage > 0) {
            List<Long> cityIds = cityRepo.findIdsByCoordinatesId(coordId);
            throw new DeletionBlockedException("Coordinates", coordId, usage, cityIds);
        }
        repo.deleteById(coordId);
        ws.sendChange("Coordinates", ChangeAction.DELETED, coordId, null);
    }
}

