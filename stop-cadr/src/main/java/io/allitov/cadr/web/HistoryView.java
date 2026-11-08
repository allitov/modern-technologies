package io.allitov.cadr.web;

/**
 * Строка таблицы истории выдач (даты форматируются как {@code dd.MM.yyyy},
 * пустая дата — «—»).
 *
 * @param employeeName ФИО фотографа.
 * @param issuedDate   дата выдачи или «—».
 * @param returnedDate дата возврата или «—».
 * @param active       выдача ещё активна.
 */
public record HistoryView(String employeeName, String issuedDate, String returnedDate, boolean active) {
}
