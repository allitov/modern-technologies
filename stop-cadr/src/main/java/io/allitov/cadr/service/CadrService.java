package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.Assignment;
import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssetRepository;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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

    @Transactional
    public void duplicateAsset(long sourceId, String newInv) {
        Asset source = assetRepository.findById(sourceId)
                .orElseThrow(() -> new DomainError("Техника не найдена"));
        if (assetRepository.existsByInventoryNumber(newInv)) {
            throw new DomainError("Инвентарный номер " + newInv + " уже существует");
        }
        Asset copy = new Asset();
        copy.setInventoryNumber(newInv);
        copy.setModel(source.getModel());
        copy.setCategory(source.getCategory());
        copy.setAssigned(false);
        assetRepository.save(copy);
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
     * Поиск техники по категории, признаку «только свободные» и подстроке
     * модели или инвентарного номера.
     */
    @Transactional(readOnly = true)
    public List<Asset> searchAssets(String category, boolean onlyFree, String text) {
        Specification<Asset> spec = (_, _, cb) -> cb.conjunction();

        if (category != null && !category.isEmpty()) {
            spec = spec.and((root, _, cb) -> cb.equal(root.get("category"), category));
        }

        if (onlyFree) {
            spec = spec.and((root, _, cb) -> cb.isFalse(root.get("assigned")));
        }

        if (text != null && !text.isEmpty()) {
            String pattern = "%" + text + "%";
            spec = spec.and((root, _, cb) -> cb.or(
                    cb.like(root.get("model"), pattern),
                    cb.like(root.get("inventoryNumber"), pattern)));
        }

        return assetRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));
    }

    /**
     * Страница списка техники с текущим держателем каждой единицы
     * и признаком следующей страницы.
     */
    @Transactional(readOnly = true)
    public PagedAssets listAssetsPaged(int page, int pageSize, String statusFilter) {
        Specification<Asset> spec = (root, query, cb) -> cb.conjunction();

        if ("В стойке".equals(statusFilter)) {
            spec = spec.and((root, query, cb) -> cb.isFalse(root.get("assigned")));
        } else if ("На съёмке".equals(statusFilter)) {
            spec = spec.and((root, query, cb) -> cb.isTrue(root.get("assigned")));
        }

        List<Asset> all = assetRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "id"));

        int offset = page * pageSize;
        int end = Math.min(offset + pageSize, all.size());

        List<AssetRow> items = new ArrayList<>();
        for (int i = offset; i < end; i++) {
            Asset asset = all.get(i);
            String holder = "";
            if (asset.isAssigned()) {
                holder = assignmentRepository.findByAssetIdAndReturnedAtIsNull(asset.getId())
                        .map(asg -> asg.getEmployee().getFullName())
                        .orElse("");
            }
            items.add(new AssetRow(asset, holder));
        }

        return new PagedAssets(items, offset + pageSize < all.size());
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

    /**
     * Техника, находящаяся сейчас у фотографа (активные выдачи),
     * отсортированная по {@code id} по убыванию.
     */
    @Transactional(readOnly = true)
    public List<Asset> employeeAssets(long employeeId) {
        return assignmentRepository.findByEmployeeIdAndReturnedAtIsNull(employeeId).stream()
                .map(asg -> assetRepository.findById(asg.getAsset().getId()).orElseThrow())
                .sorted(Comparator.comparingLong(Asset::getId).reversed())
                .toList();
    }

    // ------------------------------------------------------------
    // Передать выданную технику другому фотографу.
    // Ошибки: "Техника не выдана — используйте «Выдать»",
    //         "Нельзя переназначить уволенному фотографу",
    //         "Нет активной выдачи",
    //         "Техника уже у этого фотографа".
    // ------------------------------------------------------------
    public void reassignAsset(long assetId, long newEmployeeId) {
        throw new UnsupportedOperationException("Service.reassignAsset not implemented");
    }

    // ------------------------------------------------------------
    // Удалить технику и все её выдачи. Только если не выдана.
    // Ошибка: "Нельзя удалить выданную технику — сначала примите возврат".
    // ------------------------------------------------------------
    public void deleteAssetCascade(long assetId) {
        throw new UnsupportedOperationException("Service.deleteAssetCascade not implemented");
    }
}
