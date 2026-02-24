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
    private final InfraFailureSimulationService infraFailures;

    public PurgeService(CityRepository cityRepo,
                        HumanRepository humanRepo,
                        CoordinatesRepository coordinatesRepo,
                        InfraFailureSimulationService infraFailures) {
        this.cityRepo = cityRepo;
        this.humanRepo = humanRepo;
        this.coordinatesRepo = coordinatesRepo;
        this.infraFailures = infraFailures;
    }

    @Transactional
    public void purgeAll() {
        infraFailures.assertPostgresAvailable();
        cityRepo.deleteAllInBatch();
        humanRepo.deleteAllInBatch();
        coordinatesRepo.deleteAllInBatch();
    }
}