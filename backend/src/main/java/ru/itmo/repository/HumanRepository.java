package ru.itmo.repository;

import ru.itmo.domain.Human;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.Nullable;

public interface HumanRepository extends JpaRepository<Human, Long> {

    Page<Human> findAll(@Nullable Specification<Human> spec, Pageable pageable);
}
