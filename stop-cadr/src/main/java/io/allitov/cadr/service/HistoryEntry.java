package io.allitov.cadr.service;

import io.allitov.cadr.domain.Assignment;

/**
 * Запись истории выдач.
 *
 * @param assignment   выдача техники.
 * @param employeeName ФИО фотографа, получившего технику.
 */
public record HistoryEntry(Assignment assignment, String employeeName) {
}
