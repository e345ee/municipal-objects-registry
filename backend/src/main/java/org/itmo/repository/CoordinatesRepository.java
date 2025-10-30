package org.itmo.repository;

import org.itmo.domain.Coordinates;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {


    Page<Coordinates> findAll(@Nullable Specification<Coordinates> spec, Pageable pageable);
}
