package ru.itmo.repository;


import org.springframework.data.jpa.repository.Modifying;
import ru.itmo.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.domain.Coordinates;
import ru.itmo.domain.Human;

import java.util.List;
import java.util.Optional;

public interface CityRepository extends JpaRepository<City, Long>, JpaSpecificationExecutor<City> {

    long countByGovernorId(Long humanId);
    long countByCoordinatesId(Long coordinatesId);

    @Query("select c.id from City c where c.governor.id = :humanId")
    List<Long> findIdsByGovernorId(@Param("humanId") Long humanId);

    @Query("select c.id from City c where c.coordinates.id = :coordId")
    List<Long> findIdsByCoordinatesId(@Param("coordId") Long coordId);

    @Query("SELECT c.coordinates FROM City c WHERE c.coordinates.id = :coordId")
    Optional<Coordinates> findCoordinatesById(@Param("coordId") Long coordId);

    @Query("SELECT c.governor FROM City c WHERE c.governor.id = :governorId")
    Optional<Human> findGovernorById(@Param("governorId") Long governorId);


    @Modifying
    @Query("DELETE FROM Human h WHERE h.id = :humanId")
    void deleteHumanEntityById(@Param("humanId") Long humanId);

    @Modifying
    @Query("DELETE FROM Coordinates c WHERE c.id = :coordId")
    void deleteCoordinatesEntityById(@Param("coordId") Long coordId);

}
