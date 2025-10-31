package org.itmo.service;


import org.itmo.domain.City;
import org.itmo.dto.CityDto;
import org.itmo.repository.CityRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class CityAnalyticsService {

    private final CityRepository cityRepo;

    public CityAnalyticsService(CityRepository cityRepo) {
        this.cityRepo = cityRepo;
    }


    @Transactional(readOnly = true)
    public double averageTelephoneCode() {
        var codes = cityRepo.findAll().stream()
                .map(City::getTelephoneCode)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .toArray();
        if (codes.length == 0) return 0.0d;
        long sum = 0;
        for (int c : codes) sum += c;
        return (double) sum / codes.length;
    }


    @Transactional(readOnly = true)
    public List<CityDto> findByNameStartsWith(String prefix) {
        if (prefix == null) prefix = "";
        final String p = prefix;
        return cityRepo.findAll().stream()
                .filter(c -> c.getName() != null && c.getName().startsWith(p))
                .map(CityDto::fromEntity)
                .collect(Collectors.toList());
    }


    @Transactional(readOnly = true)
    public List<Integer> uniqueMetersAboveSeaLevel() {
        return cityRepo.findAll().stream()
                .map(City::getMetersAboveSeaLevel)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new TreeSet<Integer>()))
                .stream()
                .toList();
    }


    @Transactional(readOnly = true)
    public double distanceToLargestAreaCity(double fromX, double fromY) {
        Optional<City> maxAreaCity = cityRepo.findAll().stream()
                .filter(c -> c.getArea() != null)
                .max(Comparator.comparingInt(City::getArea));

        City city = maxAreaCity.orElseThrow(() -> new NoSuchElementException("Нет городов с заполненным area"));
        if (city.getCoordinates() == null)
            throw new NoSuchElementException("У города с max area отсутствуют координаты");

        double dx = city.getCoordinates().getX() - fromX;
        double dy = city.getCoordinates().getY() - fromY;
        return Math.sqrt(dx * dx + dy * dy);
    }


    @Transactional(readOnly = true)
    public double distanceFromOriginToOldestCity() {
        Optional<City> oldest = cityRepo.findAll().stream()
                .filter(c -> c.getEstablishmentDate() != null)
                .min(Comparator.comparing(City::getEstablishmentDate));

        City city = oldest.orElseThrow(() -> new NoSuchElementException("Нет городов с establishmentDate"));
        if (city.getCoordinates() == null)
            throw new NoSuchElementException("У самого старого города отсутствуют координаты");

        double x = city.getCoordinates().getX();
        double y = city.getCoordinates().getY();
        return Math.sqrt(x * x + y * y);
    }
}