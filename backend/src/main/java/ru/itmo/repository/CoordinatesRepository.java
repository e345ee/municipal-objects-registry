package ru.itmo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.domain.Coordinates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

import java.util.List;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {


    Page<Coordinates> findAll(@Nullable Specification<Coordinates> spec, Pageable pageable);

    @Query("SELECT COUNT(c) FROM City c WHERE c.coordinates.id = :coordId")
    long countCityUsageByCoordinatesId(@Param("coordId") Long coordId);

    @Query("SELECT c.id FROM City c WHERE c.coordinates.id = :coordId")
    List<Long> findCityIdsByCoordinatesId(@Param("coordId") Long coordId);

    boolean existsByXAndY(Float x, Float y);

    boolean existsByXAndYAndIdNot(Float x, Float y, Long id);

}
