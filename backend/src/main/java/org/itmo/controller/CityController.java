package org.itmo.controller;

import org.itmo.dto.CityDto;
import org.itmo.service.CityService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public List<CityDto> list() {
        return service.list();
    }


    @PostMapping
    public CityDto create(@Valid @RequestBody CityDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public CityDto update(@PathVariable("id") Long id, @Valid @RequestBody CityDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public CityDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }
}
