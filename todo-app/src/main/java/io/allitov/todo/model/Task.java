package io.allitov.todo.model;

import lombok.Data;

/**
 * Представляет задачу в менеджере задач.
 */
@Data
public class Task {

    private int id;

    private String title;

    private String description = "";

    private TaskStatus status = TaskStatus.TODO;

    private TaskPriority priority = TaskPriority.MEDIUM;

    /**
     * Создает задачу с указанным идентификатором и названием.
     *
     * @param id идентификатор задачи.
     * @param title название задачи.
     */
    public Task(int id, String title) {
        this.id = id;
        this.title = title;
    }
}
