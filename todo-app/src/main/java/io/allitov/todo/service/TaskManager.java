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
     */
    void addTask(String title, String description, TaskPriority priority, TaskStatus status);

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
     * Удаляет проект по его индексу.
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
}
