package ru.itmo.controller;

import jakarta.validation.Valid;
import ru.itmo.page.CoordinatesPageRequest;
import ru.itmo.page.PageDto;
import ru.itmo.dto.CoordinatesDto;
import ru.itmo.service.CoordinatesService;
import org.springframework.data.domain.Pageable;
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