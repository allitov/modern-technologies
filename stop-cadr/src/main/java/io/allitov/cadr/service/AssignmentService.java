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
import java.util.Comparator;
import java.util.List;

/**
 * Сервис работы с выдачами техники: выдача, возврат, переназначение и история.
 */
@Service
@RequiredArgsConstructor
public class AssignmentService {

    private final AssignmentRepository assignmentRepository;
    private final AssetRepository assetRepository;
    private final EmployeeRepository employeeRepository;

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

    @Transactional
    public void issueMultiple(List<Long> assetIds, long employeeId) {
        if (assetIds == null || assetIds.isEmpty()) {
            throw new DomainError("Не выбрано ни одной единицы техники");
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() -> new DomainError("Фотограф не найден"));
        if (!employee.isActive()) {
            throw new DomainError("Нельзя выдать уволенному фотографу");
        }

        List<Asset> assets = assetRepository.findAllById(assetIds);
        for (Asset asset : assets) {
            if (asset.isAssigned()) {
                throw new DomainError("Техника " + asset.getInventoryNumber() + " уже выдана");
            }
        }

        long issuedAt = Instant.now().getEpochSecond();
        for (Asset asset : assets) {
            asset.setAssigned(true);
            assetRepository.save(asset);

            Assignment assignment = new Assignment();
            assignment.setAsset(asset);
            assignment.setEmployee(employee);
            assignment.setIssuedAt(issuedAt);
            assignment.setReturnedAt(null);
            assignmentRepository.save(assignment);
        }
    }

    /**
     * История выдач единицы техники с ФИО каждого держателя,
     * отсортированная по дате выдачи по убыванию.
     */
    @Transactional(readOnly = true)
    public List<HistoryEntry> assetHistory(long assetId) {
        return assignmentRepository.findByAssetId(assetId).stream()
                .map(asg -> new HistoryEntry(asg, asg.getEmployee().getFullName()))
                .sorted(Comparator.comparingLong(
                        (HistoryEntry e) -> e.assignment().getIssuedAt()).reversed())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<Asset> employeeAssets(long employeeId) {
        return assignmentRepository.findByEmployeeIdAndReturnedAtIsNull(employeeId).stream()
                .map(asg -> assetRepository.findById(asg.getAsset().getId()).orElseThrow())
                .sorted(Comparator.comparingLong(Asset::getId).reversed())
                .toList();
    }

    @Transactional
    public void reassignAsset(long assetId, long newEmployeeId) {
        Assignment active = assignmentRepository.findByAssetIdAndReturnedAtIsNull(assetId)
                .orElseThrow(() -> new DomainError("Нет активной выдачи"));

        Asset asset = active.getAsset();
        if (!asset.isAssigned()) {
            throw new DomainError("Техника не выдана — используйте «Выдать»");
        }
        Employee currentEmployee = active.getEmployee();
        if (currentEmployee.getId() == newEmployeeId) {
            throw new DomainError("Техника уже у этого фотографа");
        }

        Employee newEmployee = employeeRepository.findById(newEmployeeId)
                .orElseThrow(() -> new DomainError("Фотограф не найден"));
        if (!newEmployee.isActive()) {
            throw new DomainError("Нельзя переназначить уволенному фотографу");
        }

        long now = Instant.now().getEpochSecond();
        active.setReturnedAt(now);
        assignmentRepository.saveAndFlush(active);

        Assignment assignment = new Assignment();
        assignment.setAsset(asset);
        assignment.setEmployee(newEmployee);
        assignment.setIssuedAt(now);
        assignment.setReturnedAt(null);
        assignmentRepository.save(assignment);
    }
}
