package org.itmo.service;

import jakarta.persistence.EntityNotFoundException;
import org.itmo.domain.Coordinates;
import org.itmo.dto.CoordinatesDto;
import org.itmo.repository.CoordinatesRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class CoordinatesService {

    private  final CoordinatesRepository repo;

    public CoordinatesService(CoordinatesRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public CoordinatesDto create(CoordinatesDto coordinatesDto) {
        Coordinates coordinates = coordinatesDto.toNewEntity();

        coordinates = repo.save(coordinates);
        return CoordinatesDto.fromEntity(coordinates);
    }

    @Transactional
    public CoordinatesDto update(Long id, CoordinatesDto coordinatesDto) {
        Coordinates coordinates = repo.findById(id).orElseThrow(() -> new EntityNotFoundException("Coordinates Not Found"));
        coordinates = coordinatesDto.toNewEntity();
        coordinates = repo.save(coordinates);
        return CoordinatesDto.fromEntity(coordinates);
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
    public void delete(Long id) {
        if (repo.existsById(id)) {
            throw new EntityNotFoundException("Coordinates Not Found");
        }
        repo.deleteById(id);
    }
}

