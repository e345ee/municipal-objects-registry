package ru.itmo.controller;

import jakarta.validation.Valid;
import ru.itmo.dto.HumanPageDto;
import ru.itmo.dto.CityPageDto;
import ru.itmo.dto.HumanDto;
import ru.itmo.service.HumanService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/humans")
public class HumanController {
    private HumanService service;

    public HumanController(HumanService service) {
        this.service = service;
    }

    @GetMapping
    public CityPageDto<HumanDto> page(
            @ModelAttribute HumanPageDto rq,
            org.springframework.data.domain.Pageable pageable
    ) {
        return service.page(rq, pageable);
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