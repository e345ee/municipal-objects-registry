package org.itmo.repository;

import org.itmo.domain.Coordinates;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CoordinatesRepository extends JpaRepository<Coordinates, Long> {
}
