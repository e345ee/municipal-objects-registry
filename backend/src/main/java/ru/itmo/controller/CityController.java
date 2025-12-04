package ru.itmo.controller;


import jakarta.validation.Valid;
import ru.itmo.page.CityPageRequest;
import ru.itmo.page.PageDto;
import ru.itmo.dto.CityDto;
import ru.itmo.service.CityService;
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

    @GetMapping("/avg-telephone-code")
    public double averageTelephoneCode() {
        return service.averageTelephoneCode();
    }


    @GetMapping("/names-starting")
    public List<CityDto> namesStartingWith(@RequestParam(name = "prefix") String prefix) {
        return service.findByNameStartsWith(prefix);
    }


    @GetMapping("/meters-above-sea-level/unique")
    public List<Integer> uniqueMetersAboveSeaLevel() {
        return service.uniqueMetersAboveSeaLevel();
    }


    @GetMapping("/distance-to-largest")
    public double distanceToLargest(@RequestParam(name = "x") double x,
                                    @RequestParam(name = "y") double y) {
        return service.distanceToLargestAreaCity(x, y);
    }


    @GetMapping("/distance-from-origin-to-oldest")
    public double distanceFromOriginToOldest() {
        return service.distanceFromOriginToOldestCity();
    }

}
