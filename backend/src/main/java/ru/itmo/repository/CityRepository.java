package ru.itmo.repository;


import ru.itmo.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {

    long countByGovernor_Id(Long humanId);
    long countByCoordinates_Id(Long coordinatesId);

    @Query("select c.id from City c where c.governor.id = :humanId")
    List<Long> findIdsByGovernorId(@Param("humanId") Long humanId);

    @Query("select c.id from City c where c.coordinates.id = :coordId")
    List<Long> findIdsByCoordinatesId(@Param("coordId") Long coordId);
}
