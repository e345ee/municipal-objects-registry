package ru.itmo.repository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.itmo.domain.Human;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

import java.util.List;

public interface HumanRepository extends JpaRepository<Human, Long> {

    Page<Human> findAll(@Nullable Specification<Human> spec, Pageable pageable);

    @Query("SELECT COUNT(c) FROM City c WHERE c.governor.id = :humanId")
    long countCityUsageByGovernorId(@Param("humanId") Long humanId);

    @Query("SELECT c.id FROM City c WHERE c.governor.id = :humanId")
    List<Long> findCityIdsByGovernorId(@Param("humanId") Long humanId);
}
