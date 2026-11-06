package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.Assignment;
import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssetRepository;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Сервис учёта техники студии.
 */
@Service
@RequiredArgsConstructor
public class CadrService {

    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
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
    public List<AssetRow> assets() {
        Map<Long, String> holders = assignmentRepository.findByReturnedAtIsNull().stream()
                .collect(Collectors.toMap(
                        a -> a.getAsset().getId(),
                        a -> a.getEmployee().getFullName(),
                        (first, _) -> first));

        return assetRepository.findAll().stream()
                .map(asset -> new AssetRow(asset, holders.getOrDefault(asset.getId(), "")))
                .toList();
    }

    @Transactional
    public void addAsset(String inv, String model, String category) {
        if (assetRepository.existsByInventoryNumber(inv)) {
            throw new DomainError("Инвентарный номер " + inv + " уже существует");
        }
        Asset a = new Asset();
        a.setInventoryNumber(inv);
        a.setModel(model);
        a.setCategory(category);
        a.setAssigned(false);
        assetRepository.save(a);
    }

    @Transactional
    public void issue(long assetId, long employeeId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new DomainError("Техника не найдена"));
        if (asset.isAssigned()) {
            throw new DomainError("Техника уже выдана");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new DomainError("Фотограф не найден"));
        if (!employee.isActive()) {
            throw new DomainError("Нельзя выдать уволенному фотографу");
        }
        asset.setAssigned(true);
        assetRepository.save(asset);

        Assignment assignment = new Assignment();
        assignment.setAsset(asset);
        assignment.setEmployee(employee);
        assignment.setIssuedAt(Instant.now().getEpochSecond());
        assignment.setReturnedAt(null);
        assignmentRepository.save(assignment);
    }

    @Transactional
    public void giveBack(long assetId) {
        Assignment assignment = assignmentRepository.findByAssetIdAndReturnedAtIsNull(assetId)
                .orElseThrow(() -> new DomainError("Нет активной выдачи для этой техники"));
        assignment.setReturnedAt(Instant.now().getEpochSecond());
        assignmentRepository.save(assignment);

        Asset asset = assignment.getAsset();
        asset.setAssigned(false);
        assetRepository.save(asset);
    }

    @Transactional(readOnly = true)
    public List<Employee> activeEmployees() {
        return employeeRepository.findByActiveTrue();
    }

    @Transactional(readOnly = true)
    public List<Asset> freeAssets() {
        return assetRepository.findByAssignedFalse();
    }
}
