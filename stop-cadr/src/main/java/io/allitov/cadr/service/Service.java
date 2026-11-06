package io.allitov.cadr.service;

import io.allitov.cadr.domain.Asset;
import io.allitov.cadr.domain.DomainError;
import io.allitov.cadr.domain.Employee;
import io.allitov.cadr.repository.AssetRepository;
import io.allitov.cadr.repository.AssignmentRepository;
import io.allitov.cadr.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Сервис учёта техники студии.
 *
 * <p>Перевод класса {@code Service} из {@code include/service.h}, {@code src/service.cpp}
 * (ODB заменён на spring-data-jpa). Методы, оставленные без реализации в C++-версии,
 * оставлены без реализации и здесь.</p>
 *
 * <p>Аннотация стереотипа указана полностью квалифицированным именем, потому что
 * короткий импорт {@code org.springframework.stereotype.Service} конфликтовал бы
 * с именем класса.</p>
 */
@org.springframework.stereotype.Service
@RequiredArgsConstructor
public class Service {

    private final EmployeeRepository employeeRepository;
    private final AssetRepository assetRepository;
    private final AssignmentRepository assignmentRepository;

    // ==================== Фотографы ====================

    /**
     * ГОТОВО (образец): список всех фотографов.
     */
    @Transactional(readOnly = true)
    public List<Employee> employees() {
        return employeeRepository.findAll();
    }

    /**
     * ГОТОВО (образец): добавление фотографа с проверкой уникальности email.
     */
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

    /**
     * ЗАДАЧА: обновление данных фотографа.
     *
     * <p>Перевод {@code Service::updateEmployee} — в C++-версии реализации нет.
     * Вызывается из диалога редактирования после клика «Сохранить».
     * Меняет ФИО, email и статус «в штате»/«уволен».</p>
     *
     * <p>Контракт:</p>
     * <ul>
     *   <li>Вход: id — существующий фотограф; name, email — новые значения;
     *       active — {@code true} (в штате) или {@code false} (уволен).</li>
     *   <li>Успех: поля в БД обновлены, ничего не возвращается (void).</li>
     *   <li>Ошибки — выбросить {@link DomainError}:
     *       1) если email изменился и такой email уже есть у другого фотографа,
     *          текст: «Фотограф с email &lt;email&gt; уже существует»;
     *       2) если active становится {@code false}, а за фотографом есть техника
     *          на съёмке, текст: «У фотографа на руках техника — сначала
     *          примите возврат».</li>
     *   <li>При любой из ошибок изменения в БД не применяются.</li>
     * </ul>
     */
    public void updateEmployee(long id, String name, String email, boolean active) {
        throw new UnsupportedOperationException("Service.updateEmployee not implemented");
    }

    // ==================== Техника ====================

    /**
     * ЗАДАЧА: список всей техники с указанием текущего держателя.
     *
     * <p>Перевод {@code Service::assets} — в C++-версии реализации нет.
     * Вызывается при каждой перерисовке вкладки «Техника».</p>
     *
     * <p>Контракт:</p>
     * <ul>
     *   <li>Вход: нет.</li>
     *   <li>Успех: возвращает {@code List<AssetRow>}, по одному элементу
     *       на каждую единицу техники в БД. В поле holder — ФИО фотографа,
     *       если техника сейчас на съёмке, или пустая строка, если в стойке.</li>
     *   <li>Ошибок не бросает (пустая БД — пустой список).</li>
     * </ul>
     */
    public List<AssetRow> assets() {
        throw new UnsupportedOperationException("Service.assets not implemented");
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

    // ==================== Для диалогов ====================

    /**
     * ГОТОВО (образец): список активных фотографов для выпадающего списка.
     */
    @Transactional(readOnly = true)
    public List<Employee> activeEmployees() {
        return employeeRepository.findByActiveTrue();
    }

    /**
     * ГОТОВО (образец): список свободной техники для выпадающего списка.
     */
    @Transactional(readOnly = true)
    public List<Asset> freeAssets() {
        return assetRepository.findByAssignedFalse();
    }
}
