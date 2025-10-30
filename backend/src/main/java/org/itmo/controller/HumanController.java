package org.itmo.controller;

import jakarta.validation.Valid;
import org.itmo.api.HumanPageRequest;
import org.itmo.api.PageDto;
import org.itmo.dto.CoordinatesDto;
import org.itmo.dto.HumanDto;
import org.itmo.service.CoordinatesService;
import org.itmo.service.HumanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/humans")
public class HumanController {
    private HumanService service;

    public HumanController(HumanService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<HumanDto> page(
            @ModelAttribute HumanPageRequest rq,   // фильтры: id, height
            org.springframework.data.domain.Pageable pageable
    ) {
        return service.page(rq, pageable);
    }

    @GetMapping("/all")
    public List<HumanDto> list() {
        return service.list();
    }

    @PostMapping
    public HumanDto create(@Valid @RequestBody HumanDto dto) {
        return service.create(dto);
    }

    @PutMapping("/{id}")
    public HumanDto update(@PathVariable("id") Long id, @Valid @RequestBody HumanDto dto) {
        return service.update(id, dto);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public HumanDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }
}