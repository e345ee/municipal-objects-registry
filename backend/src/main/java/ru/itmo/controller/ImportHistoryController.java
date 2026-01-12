package ru.itmo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import ru.itmo.dto.ImportOperationDto;
import ru.itmo.service.ImportOperationService;

@RestController
@RequestMapping(value = "/api/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImportHistoryController {

    private final ImportOperationService service;

    public ImportHistoryController(ImportOperationService service) {
        this.service = service;
    }

    @GetMapping
    public Page<ImportOperationDto> list(Pageable pageable) {
        return service.list(pageable).map(ImportOperationDto::fromEntity);
    }
}

