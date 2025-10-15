package org.itmo.service;

import org.itmo.domain.City;
import org.itmo.dto.CityDto;
import org.itmo.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CityService {
    private final CityRepository repo;

    public CityService(CityRepository repo) {
        this.repo = repo;
    }

    @Transactional(readOnly = true)
    public List<CityDto> list() {
        return repo.findAll().stream().map(CityDto::fromEntity).toList();
    }

    @Transactional(readOnly = true)
    public CityDto get(Long id) {
        City c = repo.findById(id).orElseThrow();
        return CityDto.fromEntity(c);
    }

    @Transactional
    public CityDto create(CityDto dto) {
        City saved = repo.save(dto.toNewEntity());
        return CityDto.fromEntity(saved);
    }

    @Transactional
    public CityDto update(Long id, CityDto dto) {
        City c = repo.findById(id).orElseThrow();
        dto.applyToEntity(c);
        return CityDto.fromEntity(repo.save(c));
    }

    @Transactional
    public void delete(Long id) {
        repo.deleteById(id);
    }

}
