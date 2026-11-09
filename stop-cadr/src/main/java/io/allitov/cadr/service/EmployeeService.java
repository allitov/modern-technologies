package io.allitov.cadr.service;

import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис работы с фотографами.
 */
@Service
@RequiredArgsConstructor
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final AssignmentRepository assignmentRepository;

    @Transactional(readOnly = true)
    public List<Employee> employees() {
        return employeeRepository.findAll();
    }

    @Transactional
    public void addEmployee(String name, String email) {
        if (employeeRepository.existsByEmail(email)) {
            throw new DomainError("Фотограф с email " + email + " уже существует");
        }
        Employee e = new Employee();
        e.setFullName(name);
        e.setEmail(email);
        e.setActive(true);
        employeeRepository.save(e);
    }

    public void setEmployeeActive(long id, boolean active) {
        throw new UnsupportedOperationException("Service.setEmployeeActive not implemented");
    }

    @Transactional
    public void updateEmployee(long id, String name, String email, boolean active) {
        Employee e = employeeRepository.findById(id)
                .orElseThrow(() -> new DomainError("Фотограф не найден"));
        if (!e.getEmail().equals(email) && employeeRepository.existsByEmailAndIdNot(email, id)) {
            throw new DomainError("Фотограф с email " + email + " уже существует");
        }
        if (!active && assignmentRepository.existsByEmployeeIdAndReturnedAtIsNull(id)) {
            throw new DomainError("У фотографа на руках техника — сначала примите возврат");
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
