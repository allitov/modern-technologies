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

    private int projectId;

    /**
     * Создает задачу с указанным идентификатором и названием.
     *
     * @param id идентификатор задачи.
     * @param title название задачи.
     */
    public Task(int id, String title) {
        this(id, title, 0);
    }

    /**
     * Создает задачу с указанным идентификатором, названием и проектом.
     *
     * @param id идентификатор задачи.
     * @param title название задачи.
     * @param projectId идентификатор проекта; значение {@code 0} означает отсутствие проекта.
     */
    public Task(int id, String title, int projectId) {
        this.id = id;
        this.title = title;
        this.projectId = projectId;
    }
}
