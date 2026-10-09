package io.allitov.todo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Определяет возможные статусы задачи.
 */
@Getter
@RequiredArgsConstructor
public enum TaskStatus {

    /**
     * Задача еще не начата.
     */
    TODO("Todo"),

    /**
     * Задача находится в работе.
     */
    IN_PROGRESS("In Progress"),

    /**
     * Задача завершена.
     */
    DONE("Done");

    private final String displayName;

    @Override
    public String toString() {
        return displayName;
    }
}
