package org.itmo.repository;

import org.itmo.domain.Human;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HumanRepository extends JpaRepository<Human, Long> {}
