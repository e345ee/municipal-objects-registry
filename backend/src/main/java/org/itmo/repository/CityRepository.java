package org.itmo.repository;

import org.itmo.domain.City;
import org.springframework.data.jpa.repository.JpaRepository;
public interface CityRepository extends JpaRepository<City, Long> {}
