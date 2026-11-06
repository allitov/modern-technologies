package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Доступ к таблице {@code employees}.
 */
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    boolean existsByEmail(String email);

    List<Employee> findByActiveTrue();
}
