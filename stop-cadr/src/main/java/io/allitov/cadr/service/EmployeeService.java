package io.allitov.cadr.service;

import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

/**
 * Сервис работы с фотографами.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;

    private static final Supplier<DomainError> EMPLOYEE_NOT_FOUND =
            () -> new DomainError("Фотограф не найден");
    private static final Supplier<DomainError> EMPLOYEE_HAS_ASSIGNED_ASSETS =
            () -> new DomainError("У фотографа на руках техника — сначала примите возврат");
    private static final Supplier<UnsupportedOperationException> SET_EMPLOYEE_ACTIVE_NOT_IMPLEMENTED =
            () -> new UnsupportedOperationException("Service.setEmployeeActive not implemented");

    private static Supplier<DomainError> emailAlreadyExists(String email) {
        return () -> new DomainError("Фотограф с email %s уже существует".formatted(email));
    }

    @Transactional(readOnly = true)
    public List<Employee> employees() {
        return employeeRepository.findAll();
    }

    @Transactional
    public void addEmployee(String name, String email) {
        if (employeeRepository.existsByEmail(email)) {
            throw emailAlreadyExists(email).get();
        }
        Employee e = new Employee();
        e.setFullName(name);
        e.setEmail(email);
        e.setActive(true);
        employeeRepository.save(e);
    }

    public void setEmployeeActive(long id, boolean active) {
        throw SET_EMPLOYEE_ACTIVE_NOT_IMPLEMENTED.get();
    }

    @Transactional
    public void updateEmployee(long id, String name, String email, boolean active) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(EMPLOYEE_NOT_FOUND);
        if (!e.getEmail().equals(email) && employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw emailAlreadyExists(email).get();
        }
        if (!active && assignmentRepository.existsByEmployeeIdAndReturnedAtIsNull(id)) {
            throw EMPLOYEE_HAS_ASSIGNED_ASSETS.get();
        }
        e.setFullName(name);
        e.setEmail(email);
        e.setActive(active);
        employeeRepository.save(e);
    }

    @Transactional(readOnly = true)
    public List<Employee> activeEmployees() {
        return employeeRepository.findByActiveTrue();
    }
}
