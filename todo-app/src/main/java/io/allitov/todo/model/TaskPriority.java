package io.allitov.todo.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Определяет возможные приоритеты задачи.
 */
@Getter
@RequiredArgsConstructor
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

    @Override
    public String toString() {
        return displayName;
    }
}
