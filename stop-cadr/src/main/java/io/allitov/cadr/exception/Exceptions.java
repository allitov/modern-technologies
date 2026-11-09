package io.allitov.cadr.exception;

import lombok.experimental.UtilityClass;

import java.util.function.Supplier;

@UtilityClass
public class Exceptions {

    public Supplier<DomainError> ASSET_NOT_FOUND = () -> new DomainError("Техника не найдена");
    public Supplier<DomainError> CANNOT_DELETE_ASSIGNED_ASSET = () -> new DomainError("Нельзя удалить выданную технику — сначала примите возврат");
    public Supplier<DomainError> ASSET_ALREADY_ISSUED = () -> new DomainError("Техника уже выдана");
    public Supplier<DomainError> EMPLOYEE_NOT_FOUND = () -> new DomainError("Фотограф не найден");
    public Supplier<DomainError> CANNOT_ISSUE_TO_INACTIVE_EMPLOYEE = () -> new DomainError("Нельзя выдать уволенному фотографу");
    public Supplier<DomainError> NO_ACTIVE_ASSIGNMENT_FOR_ASSET = () -> new DomainError("Нет активной выдачи для этой техники");
    public Supplier<DomainError> NO_ASSETS_SELECTED = () -> new DomainError("Не выбрано ни одной единицы техники");
    public Supplier<DomainError> NO_ACTIVE_ASSIGNMENT = () -> new DomainError("Нет активной выдачи");
    public Supplier<DomainError> ASSET_NOT_ISSUED = () -> new DomainError("Техника не выдана — используйте «Выдать»");
    public Supplier<DomainError> ASSET_ALREADY_AT_THIS_EMPLOYEE = () -> new DomainError("Техника уже у этого фотографа");
    public Supplier<DomainError> CANNOT_REASSIGN_TO_INACTIVE_EMPLOYEE = () -> new DomainError("Нельзя переназначить уволенному фотографу");
    public Supplier<DomainError> EMPLOYEE_HAS_ASSIGNED_ASSETS = () -> new DomainError("У фотографа на руках техника — сначала примите возврат");
    public Supplier<UnsupportedOperationException> SET_EMPLOYEE_ACTIVE_NOT_IMPLEMENTED = () -> new UnsupportedOperationException("Service.setEmployeeActive not implemented");

    public Supplier<DomainError> emailAlreadyExists(String email) {
        return () -> new DomainError("Фотограф с email %s уже существует".formatted(email));
    }
    
    public Supplier<DomainError> assetAlreadyIssued(String inventoryNumber) {
        return () -> new DomainError("Техника %s уже выдана".formatted(inventoryNumber));
    }

    public Supplier<DomainError> inventoryNumberAlreadyExists(String inv) {
        return () -> new DomainError("Инвентарный номер %s уже существует".formatted(inv));
    }
}
