package org.itmo.controller;

import jakarta.validation.Valid;
import org.itmo.api.CoordinatesPageRequest;
import org.itmo.api.PageDto;
import org.itmo.dto.CityDto;
import org.itmo.dto.CoordinatesDto;
import org.itmo.service.CoordinatesService;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/coordinates")
public class CoordinatesController {
    private CoordinatesService service;

    public CoordinatesController(CoordinatesService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<CoordinatesDto> page(
            @ModelAttribute CoordinatesPageRequest rq,
            Pageable pageable
    ) {
        return service.page(rq, pageable);
    }

    @GetMapping("/all")
    public List<CoordinatesDto> list() {
        return service.list();
    }
    @PostMapping
    public CoordinatesDto create(@Valid @RequestBody CoordinatesDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public CoordinatesDto update(@PathVariable("id") Long id, @Valid @RequestBody CoordinatesDto dto) {
        return service.update(id, dto);
    }


    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public CoordinatesDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }
}