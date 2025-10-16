package org.itmo.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.itmo.domain.City;
import org.itmo.domain.Climate;
import org.itmo.domain.Government;
import org.itmo.dto.CityDto;
import org.itmo.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class CityService {

    private final CityRepository repo;

    @PersistenceContext
    private EntityManager em;

    public CityService(CityRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CityDto create(CityDto dto) {
        City e = toEntity(dto);

        e.setCreationDate(null);

        e = repo.save(e);

        em.flush();
        em.refresh(e);

        return toDto(e);
    }

    @Transactional
    public CityDto update(Long id, CityDto dto) {
        City e = repo.findById(id).orElseThrow();

        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setCapital(dto.isCapital());
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));
        e.setGovernment(Government.valueOf(dto.getGovernment()));
        e.setEstablishmentDate(dto.getEstablishmentDate());

        e = repo.save(e);
        return toDto(e);
    }

    @Transactional(readOnly = true)
    public CityDto get(Long id) {
        return toDto(repo.findById(id).orElseThrow());
    }

    @Transactional(readOnly = true)
    public List<CityDto> list() {
        return repo.findAll().stream().map(this::toDto).toList();
    }


    private CityDto toDto(City e) {
        CityDto dto = new CityDto();
        dto.setId(e.getId());
        dto.setName(e.getName());
        dto.setArea(e.getArea());
        dto.setPopulation(e.getPopulation());
        dto.setCapital(e.isCapital());
        dto.setMetersAboveSeaLevel(e.getMetersAboveSeaLevel());
        dto.setTelephoneCode(e.getTelephoneCode());
        dto.setClimate(e.getClimate().name());
        dto.setGovernment(e.getGovernment().name());

        dto.setCreationDate(e.getCreationDate());
        dto.setEstablishmentDate(e.getEstablishmentDate());
        return dto;
    }

    private City toEntity(CityDto dto) {
        City e = new City();
        e.setId(dto.getId());
        e.setName(dto.getName());
        e.setArea(dto.getArea());
        e.setPopulation(dto.getPopulation());
        e.setCapital(dto.isCapital());
        e.setMetersAboveSeaLevel(dto.getMetersAboveSeaLevel());
        e.setTelephoneCode(dto.getTelephoneCode());
        e.setClimate(Climate.valueOf(dto.getClimate()));
        e.setGovernment(Government.valueOf(dto.getGovernment()));
        e.setEstablishmentDate(dto.getEstablishmentDate());
        return e;
    }

    @Transactional
    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new NoSuchElementException("Город с id=" + id + " не найден");
        }
        repo.deleteById(id);
    }
}
