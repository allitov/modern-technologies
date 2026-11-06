package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssetRepository;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Перевод {@code Service::setEmployeeActive} — в C++-версии метод объявлен
     * в {@code service.h}, но реализации в {@code service.cpp} не имеет.
     */
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

    // ==================== Выдача ====================

    /**
     * ЗАДАЧА (главная в лабе): оформление выдачи техники фотографу.
     *
     * <p>Перевод {@code Service::issue} — в C++-версии реализации нет.
     * Вызывается из диалога «Выдать» после клика «Сохранить».</p>
     *
     * <p>Контракт:</p>
     * <ul>
     *   <li>Вход: assetId, employeeId — id существующих записей.</li>
     *   <li>Успех: техника переходит в статус assigned = true,
     *       в assignments появляется запись с issuedAt = текущее время
     *       и пустым returnedAt. Ничего не возвращается (void).</li>
     *   <li>Ошибки — выбросить {@link DomainError}:
     *       1) техника уже на съёмке: «Техника уже выдана»;
     *       2) фотограф уволен: «Нельзя выдать уволенному фотографу».</li>
     *   <li>При любой ошибке изменения в БД не применяются.</li>
     *   <li>Если прервать процесс между обновлением техники и созданием записи
     *       о выдаче (например, временно сломать БД) — статус техники
     *       не должен остаться «на съёмке» (нужна одна транзакция).</li>
     * </ul>
     */
    public void issue(long assetId, long employeeId) {
        throw new UnsupportedOperationException("Service.issue not implemented");
    }

    /**
     * ЗАДАЧА: возврат техники со съёмки обратно в стойку.
     *
     * <p>Перевод {@code Service::giveBack} — в C++-версии реализации нет.
     * Вызывается из главного окна кнопкой «Принять».</p>
     *
     * <p>Контракт:</p>
     * <ul>
     *   <li>Вход: assetId — id техники.</li>
     *   <li>Успех: у активной выдачи проставляется returnedAt = текущее время,
     *       у техники assigned = false. Ничего не возвращается (void).</li>
     *   <li>Ошибка — выбросить {@link DomainError}: если активной выдачи нет
     *       (техника уже в стойке): «Нет активной выдачи для этой техники».</li>
     *   <li>При ошибке изменения в БД не применяются.</li>
     * </ul>
     */
    public void giveBack(long assetId) {
        throw new UnsupportedOperationException("Service.giveBack not implemented");
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
