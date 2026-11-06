package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Доступ к таблице {@code assignments}.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {
}
