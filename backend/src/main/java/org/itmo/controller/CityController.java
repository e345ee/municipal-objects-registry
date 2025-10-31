package org.itmo.controller;


import jakarta.validation.Valid;
import org.itmo.api.CityPageRequest;
import org.itmo.api.PageDto;
import org.itmo.dto.CityDto;
import org.itmo.service.CityService;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cities", produces = MediaType.APPLICATION_JSON_VALUE)
public class CityController {

    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping
    public PageDto<CityDto> page(@ModelAttribute CityPageRequest rq, Pageable pageable) {
        if (rq.getPage() == null) rq.setPage(pageable.getPageNumber());
        if (rq.getSize() == null) rq.setSize(pageable.getPageSize());

        if (Boolean.TRUE.equals(rq.getGovernorIdIsNull()) && rq.getGovernorId() != null) {
            throw new IllegalArgumentException("Нельзя одновременно передавать governorId и governorIdIsNull=true");
        }
        return service.page(rq);
    }

    @GetMapping("/all")
    public List<CityDto> list() {
        return service.list();
    }

    @GetMapping("/{id}")
    public CityDto get(@PathVariable Long id) { return service.get(id); }

    @PostMapping
    public CityDto create(@Valid @RequestBody CityDto dto) { return service.create(dto); }

    @PutMapping("/{id}")
    public CityDto update(@PathVariable Long id, @Valid @RequestBody CityDto dto) { return service.update(id, dto); }

    @DeleteMapping("/{id}")
    public org.springframework.http.ResponseEntity<Void> delete(
            @PathVariable Long id,
            @RequestParam(name = "deleteGovernorIfOrphan", defaultValue = "false") boolean deleteGovernorIfOrphan,
            @RequestParam(name = "deleteCoordinatesIfOrphan", defaultValue = "false") boolean deleteCoordinatesIfOrphan
    ) {
        service.delete(id, deleteGovernorIfOrphan, deleteCoordinatesIfOrphan);
        return org.springframework.http.ResponseEntity.noContent().build();
    }
}
