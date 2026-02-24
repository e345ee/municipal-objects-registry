package ru.itmo.controller;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.itmo.domain.ImportOperation;
import ru.itmo.domain.ImportStatus;
import ru.itmo.dto.ImportOperationDto;
import ru.itmo.service.ImportOperationService;
import ru.itmo.storage.ImportFileStorageService;
import ru.itmo.storage.StoredImportFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;

@RestController
@RequestMapping(value = "/api/imports", produces = MediaType.APPLICATION_JSON_VALUE)
public class ImportHistoryController {

    private final ImportOperationService service;
    private final ImportFileStorageService fileStorageService;

    public ImportHistoryController(ImportOperationService service, ImportFileStorageService fileStorageService) {
        this.service = service;
        this.fileStorageService = fileStorageService;
    }

    @GetMapping
    public Page<ImportOperationDto> list(Pageable pageable) {
        return service.list(pageable).map(ImportOperationDto::fromEntity);
    }

    @GetMapping(value = "/{id}/file", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<byte[]> downloadImportFile(@PathVariable Long id) {
        ImportOperation op = service.getRequired(id);
        if (op.getStatus() != ImportStatus.SUCCESS || op.getFileFinalKey() == null || op.getFileFinalKey().isBlank()) {
            throw new NoSuchElementException("Файл импорта для операции id=" + id + " недоступен");
        }

        StoredImportFile file = fileStorageService.download(op.getFileFinalKey());
        String filename = op.getSourceFilename();
        if (filename == null || filename.isBlank()) {
            filename = "import-" + id + ".json";
        }

        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getBytes().length)
                .body(file.getBytes());
    }
}
