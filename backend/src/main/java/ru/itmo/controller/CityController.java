package ru.itmo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.itmo.dto.*;
import ru.itmo.service.CityImportService;
import ru.itmo.service.CityService;

import java.util.List;

@RestController
@RequestMapping(value = "/api/cities", produces = MediaType.APPLICATION_JSON_VALUE)
public class CityController {

    private final CityService service;
    private final CityImportService importService;
    private final ObjectMapper objectMapper;

    public CityController(CityService service, CityImportService importService, ObjectMapper objectMapper) {
        this.service = service;
        this.importService = importService;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public CityPageDto<CityDto> page(@ModelAttribute PageRequestDto rq, Pageable pageable) {
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
    public double averageTelephoneCode() { return service.averageTelephoneCode(); }

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

    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ImportResultDto importJson(@RequestPart("file") MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Файл не передан или пустой.");
        }

        try (var is = file.getInputStream()) {
            List<CityDto> dtos = objectMapper.readValue(is, new TypeReference<List<CityDto>>() {});
            return importService.importCities(dtos);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Некорректный JSON: " + e.getOriginalMessage());
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Ошибка чтения файла: " + e.getMessage());
        }
    }
}

