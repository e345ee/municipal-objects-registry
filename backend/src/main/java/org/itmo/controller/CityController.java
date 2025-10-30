package org.itmo.controller;

import org.itmo.api.CityPageRequest;
import org.itmo.api.PageDto;
import org.itmo.dto.CityDto;
import org.itmo.service.CityService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cities", produces = MediaType.APPLICATION_JSON_VALUE)
public class CityController {
    private final CityService service;

    public CityController(CityService service) {
        this.service = service;
    }

    @GetMapping(params = {"page","size"})
    public PageDto<CityDto> page(
            @RequestParam Integer page,
            @RequestParam Integer size,

            @RequestParam(required = false) List<String> sort,

            @RequestParam(required = false) Long id,
            @RequestParam(required = false) Long coordinatesId,
            @RequestParam(required = false) Long governorId,

            @RequestParam(required = false) Boolean governorIdIsNull,

            @RequestParam(required = false) String name,
            @RequestParam(required = false) String climate,
            @RequestParam(required = false) String government,
            @RequestParam(required = false) Long population,
            @RequestParam(required = false) Integer telephoneCode,

            @RequestParam(required = false) String sortBy,
            @RequestParam(required = false, defaultValue = "asc") String dir
    ) {


        if (Boolean.TRUE.equals(governorIdIsNull) && governorId != null) {
            throw new IllegalArgumentException("Нельзя одновременно передавать governorId и governorIdIsNull=true");
        }

        CityPageRequest rq = new CityPageRequest();
        rq.setPage(page);
        rq.setSize(size);

        rq.setSort(sort);
        rq.setSortBy(sortBy);
        rq.setDir(dir);

        rq.setId(id);
        rq.setCoordinatesId(coordinatesId);
        rq.setGovernorId(governorId);
        rq.setGovernorIdIsNull(governorIdIsNull);

        rq.setName(name);
        rq.setClimate(climate);
        rq.setGovernment(government);
        rq.setPopulation(population);
        rq.setTelephoneCode(telephoneCode);

        return service.page(rq);
    }

    @GetMapping(params = {"!page","!size"})
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
    public ResponseEntity<Void> deleteCity(
            @PathVariable("id") Long id,
            @RequestParam(name = "deleteGovernorIfOrphan", defaultValue = "false") boolean deleteGovernorIfOrphan,
            @RequestParam(name = "deleteCoordinatesIfOrphan", defaultValue = "false") boolean deleteCoordinatesIfOrphan
    ) {
        service.delete(id, deleteGovernorIfOrphan, deleteCoordinatesIfOrphan);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}")
    public CityDto get(@PathVariable("id") Long id) {
        return service.get(id);
    }
}