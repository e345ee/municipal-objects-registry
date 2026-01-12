package ru.itmo.repository;


import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import ru.itmo.domain.ImportOperation;

public interface ImportOperationRepository extends JpaRepository<ImportOperation, Long> {
    Page<ImportOperation> findAllByOrderByStartedAtDesc(Pageable pageable);
}

