package org.itmo.controller;

import org.itmo.dto.CityDto;
import org.itmo.repository.CityRepository;
import org.itmo.service.CityService;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/cities")
public class CityController {
    private final CityService service;
    public CityController(CityService service){this.service = service;}

    @GetMapping public List<CityDto> list(){return service.list();}
    @GetMapping("/{id}") public CityDto get(@PathVariable Long id){return service.get(id);}
    @PostMapping public CityDto crete(@Valid @RequestBody CityDto dto){return service.create(dto);}
    @PutMapping("/{id}") CityDto update(@PathVariable Long id, @Valid @RequestBody CityDto dto){return service.update(id,dto);}
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}
