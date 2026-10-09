package io.allitov.todo.model;

import lombok.Getter;

/**
 * Определяет возможные приоритеты задачи.
 */
@Getter
public enum TaskPriority {

    /**
     * Низкий приоритет.
     */
    LOW("Low"),

    /**
     * Средний приоритет.
     */
    MEDIUM("Medium"),

    /**
     * Высокий приоритет.
     */
    HIGH("High");

    private final String displayName;

    TaskPriority(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}
