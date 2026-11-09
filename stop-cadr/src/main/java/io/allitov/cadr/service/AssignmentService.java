package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.Assignment;
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

import static io.allitov.cadr.exception.Exceptions.ASSET_ALREADY_AT_THIS_EMPLOYEE;
import static io.allitov.cadr.exception.Exceptions.ASSET_ALREADY_ISSUED;
import static io.allitov.cadr.exception.Exceptions.ASSET_NOT_FOUND;
import static io.allitov.cadr.exception.Exceptions.ASSET_NOT_ISSUED;
import static io.allitov.cadr.exception.Exceptions.CANNOT_ISSUE_TO_INACTIVE_EMPLOYEE;
import static io.allitov.cadr.exception.Exceptions.CANNOT_REASSIGN_TO_INACTIVE_EMPLOYEE;
import static io.allitov.cadr.exception.Exceptions.EMPLOYEE_NOT_FOUND;
import static io.allitov.cadr.exception.Exceptions.NO_ACTIVE_ASSIGNMENT;
import static io.allitov.cadr.exception.Exceptions.NO_ACTIVE_ASSIGNMENT_FOR_ASSET;
import static io.allitov.cadr.exception.Exceptions.NO_ASSETS_SELECTED;
import static io.allitov.cadr.exception.Exceptions.assetAlreadyIssued;

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
                .orElseThrow(ASSET_NOT_FOUND);
        if (asset.isAssigned()) {
            throw ASSET_ALREADY_ISSUED.get();
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EMPLOYEE_NOT_FOUND);
        if (!employee.isActive()) {
            throw CANNOT_ISSUE_TO_INACTIVE_EMPLOYEE.get();
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
                .orElseThrow(NO_ACTIVE_ASSIGNMENT_FOR_ASSET);
        assignment.setReturnedAt(Instant.now().getEpochSecond());
        assignmentRepository.save(assignment);

        Asset asset = assignment.getAsset();
        asset.setAssigned(false);
        assetRepository.save(asset);
    }

    @Transactional
    public void issueMultiple(List<Long> assetIds, long employeeId) {
        if (assetIds == null || assetIds.isEmpty()) {
            throw NO_ASSETS_SELECTED.get();
        }
        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(EMPLOYEE_NOT_FOUND);
        if (!employee.isActive()) {
            throw CANNOT_ISSUE_TO_INACTIVE_EMPLOYEE.get();
        }

        List<Asset> assets = assetRepository.findAllById(assetIds);
        for (Asset asset : assets) {
            if (asset.isAssigned()) {
                throw assetAlreadyIssued(asset.getInventoryNumber()).get();
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
                .orElseThrow(NO_ACTIVE_ASSIGNMENT);

        Asset asset = active.getAsset();
        if (!asset.isAssigned()) {
            throw ASSET_NOT_ISSUED.get();
        }
        Employee currentEmployee = active.getEmployee();
        if (currentEmployee.getId() == newEmployeeId) {
            throw ASSET_ALREADY_AT_THIS_EMPLOYEE.get();
        }

        Employee newEmployee = employeeRepository.findById(newEmployeeId)
                .orElseThrow(EMPLOYEE_NOT_FOUND);
        if (!newEmployee.isActive()) {
            throw CANNOT_REASSIGN_TO_INACTIVE_EMPLOYEE.get();
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
