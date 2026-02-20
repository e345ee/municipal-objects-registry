package ru.itmo.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.itmo.service.PurgeService;

@RestController
@RequestMapping("/api")
public class PurgeController {

    private final PurgeService purgeService;

    public PurgeController(PurgeService purgeService) {
        this.purgeService = purgeService;
    }

    @DeleteMapping("/purge")
    public ResponseEntity<Void> purge() {
        purgeService.purgeAll();
        return ResponseEntity.noContent().build(); // 204
    }
}