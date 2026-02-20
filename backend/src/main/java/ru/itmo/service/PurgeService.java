package ru.itmo.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.itmo.repository.CityRepository;
import ru.itmo.repository.CoordinatesRepository;
import ru.itmo.repository.HumanRepository;

@Service
public class PurgeService {

    private final CityRepository cityRepo;
    private final HumanRepository humanRepo;
    private final CoordinatesRepository coordinatesRepo;

    public PurgeService(CityRepository cityRepo,
                        HumanRepository humanRepo,
                        CoordinatesRepository coordinatesRepo) {
        this.cityRepo = cityRepo;
        this.humanRepo = humanRepo;
        this.coordinatesRepo = coordinatesRepo;
    }

    @Transactional
    public void purgeAll() {
        cityRepo.deleteAllInBatch();
        humanRepo.deleteAllInBatch();
        coordinatesRepo.deleteAllInBatch();
    }
}