package io.allitov.todo.model;

import lombok.Getter;

/**
 * Определяет возможные статусы задачи.
 */
@Getter
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

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
