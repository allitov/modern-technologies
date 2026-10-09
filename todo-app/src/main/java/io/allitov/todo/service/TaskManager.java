package io.allitov.todo.service;

import io.allitov.todo.model.Project;
import io.allitov.todo.model.Task;
import io.allitov.todo.model.TaskPriority;
import io.allitov.todo.model.TaskStatus;

import java.util.List;

/**
 * Управляет задачами и проектами.
 */
public interface TaskManager {

    /**
     * Добавляет новую задачу.
     *
     * @param title название задачи.
     * @param description описание задачи.
     * @param priority приоритет задачи.
     * @param status статус задачи.
     * @param projectId идентификатор проекта задачи; значение {@code 0} означает отсутствие проекта.
     */
    void addTask(String title, String description, TaskPriority priority, TaskStatus status, int projectId);

    /**
     * Переносит задачу в указанный проект.
     *
     * @param taskIndex индекс переносимой задачи.
     * @param projectId идентификатор целевого проекта; значение {@code 0} означает отвязку задачи от проекта.
     * @return {@code true}, если перенос выполнен; {@code false}, если индекс некорректен или целевой проект не найден.
     */
    boolean moveTaskToProject(int taskIndex, int projectId);

    /**
     * Обновляет задачу по ее индексу, сохраняя исходный идентификатор.
     *
     * @param index индекс обновляемой задачи.
     * @param title новое название задачи.
     * @param description новое описание задачи.
     * @param priority новый приоритет задачи.
     * @param status новый статус задачи.
     */
    void updateTask(int index, String title, String description, TaskPriority priority, TaskStatus status);

    /**
     * Удаляет задачу по ее индексу.
     *
     * @param index индекс удаляемой задачи.
     */
    void deleteTask(int index);

    /**
     * Возвращает список всех задач.
     *
     * @return список задач.
     */
    List<Task> getTasks();

    /**
     * Добавляет новый проект.
     *
     * @param name название проекта.
     */
    void addProject(String name);

    /**
     * Удаляет проект по его индексу вместе со всеми его задачами.
     *
     * @param index индекс удаляемого проекта.
     */
    void deleteProject(int index);

    /**
     * Возвращает список всех проектов.
     *
     * @return список проектов.
     */
    List<Project> getProjects();

    /**
     * Считает количество задач в проекте.
     *
     * @param projectId идентификатор проекта.
     * @return количество задач проекта.
     */
    int countTasksInProject(int projectId);

    /**
     * Считает количество задач проекта с указанным статусом.
     *
     * @param projectId идентификатор проекта.
     * @param status статус задач.
     * @return количество задач проекта с указанным статусом.
     */
    int countTasksInProjectByStatus(int projectId, TaskStatus status);

    /**
     * Вычисляет процент выполненных задач проекта.
     *
     * @param projectId идентификатор проекта.
     * @return процент задач со статусом {@code Done} от общего числа задач проекта; {@code 0.0}, если у проекта нет задач.
     */
    double completionPercent(int projectId);
}
