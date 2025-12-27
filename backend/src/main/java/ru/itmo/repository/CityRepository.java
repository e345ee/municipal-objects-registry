package ru.itmo.repository;

import ru.itmo.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {

    long countByGovernorId(Long humanId);

    long countByCoordinatesId(Long coordinatesId);

}
