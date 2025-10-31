package ru.itmo.controller;

import ru.itmo.dto.CityDto;
import ru.itmo.service.CityAnalyticsService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cities/analytics")
public class CityAnalyticsController {

    private final CityAnalyticsService service;

    public CityAnalyticsController(CityAnalyticsService service) {
        this.service = service;
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