package ru.itmo.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.dto.CityDto;
import ru.itmo.dto.ImportResultDto;
import ru.itmo.dto.MinioStoredFileDto;
import ru.itmo.service.CityImportService;
import ru.itmo.storage.ImportFileStorageService;
import ru.itmo.storage.StoredImportFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/api/admin/storage")
public class AdminStorageController {

    private final ImportFileStorageService fileStorageService;
    private final CityImportService cityImportService;
    private final ObjectMapper objectMapper;

    public AdminStorageController(ImportFileStorageService fileStorageService,
                                  CityImportService cityImportService,
                                  ObjectMapper objectMapper) {
        this.fileStorageService = fileStorageService;
        this.cityImportService = cityImportService;
        this.objectMapper = objectMapper;
    }

    @GetMapping(value = "/files", produces = MediaType.APPLICATION_JSON_VALUE)
    public Page<MinioStoredFileDto> listFiles(@RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "10") int size) {
        int safePage = Math.max(0, page);
        int safeSize = Math.min(100, Math.max(1, size));

        List<MinioStoredFileDto> all = fileStorageService.listCommittedFiles();
        int from = Math.min(safePage * safeSize, all.size());
        int to = Math.min(from + safeSize, all.size());

        return new PageImpl<>(all.subList(from, to), PageRequest.of(safePage, safeSize), all.size());
    }

    @GetMapping("/file")
    public ResponseEntity<byte[]> downloadByKey(@RequestParam("key") String key) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Параметр key обязателен");
        }

        StoredImportFile file = fileStorageService.download(key);
        String filename = fileNameFromKey(key);
        String encoded = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encoded)
                .contentType(MediaType.parseMediaType(file.getContentType()))
                .contentLength(file.getBytes().length)
                .body(file.getBytes());
    }

    @PostMapping(value = "/reimport", produces = MediaType.APPLICATION_JSON_VALUE)
    public ImportResultDto reimportStoredFile(@RequestParam("key") String key,
                                              @RequestParam(name = "debugFailStage", required = false) String debugFailStage) {
        if (key == null || key.isBlank()) {
            throw new IllegalArgumentException("Параметр key обязателен");
        }

        StoredImportFile file = fileStorageService.download(key);
        byte[] bytes = file.getBytes();
        if (bytes == null || bytes.length == 0) {
            throw new IllegalArgumentException("Файл в MinIO пустой");
        }

        try {
            var dtos = objectMapper.readValue(bytes, new TypeReference<List<CityDto>>() {});
            return cityImportService.importCities(dtos, bytes, fileNameFromKey(key), debugFailStage);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Некорректный JSON в файле MinIO: " + e.getOriginalMessage());
        }
    }

    private String fileNameFromKey(String key) {
        int idx = key.lastIndexOf('/');
        String filename = idx >= 0 ? key.substring(idx + 1) : key;
        if (filename.isBlank()) {
            filename = "import.json";
        }
        if (!filename.toLowerCase(Locale.ROOT).endsWith(".json")) {
            filename = filename + ".json";
        }
        return filename;
    }
}
