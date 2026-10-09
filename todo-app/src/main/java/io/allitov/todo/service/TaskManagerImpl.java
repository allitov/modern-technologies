package io.allitov.todo.service;

import io.allitov.todo.model.Project;
import io.allitov.todo.model.Task;
import io.allitov.todo.model.TaskPriority;
import io.allitov.todo.model.TaskStatus;
import io.allitov.todo.repository.Repository;
import io.allitov.todo.repository.RepositoryImpl;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Управляет задачами и проектами.
 */
@NoArgsConstructor
public class TaskManagerImpl implements TaskManager {

    private final Repository<Task> taskRepository = new RepositoryImpl<>();

    private final Repository<Project> projectRepository = new RepositoryImpl<>();

    private int nextTaskId = 1;

    private int nextProjectId = 1;

    @Override
    public void addTask(String title, String description, TaskPriority priority, TaskStatus status) {
        Task task = new Task(nextTaskId++, title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        taskRepository.add(task);
    }

    @Override
    public void updateTask(int index, String title, String description, TaskPriority priority, TaskStatus status) {
        Task task = taskRepository.getAll().get(index);
        task.setTitle(title);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        taskRepository.update(index, task);
    }

    @Override
    public void deleteTask(int index) {
        taskRepository.remove(index);
    }

    @Override
    public List<Task> getTasks() {
        return taskRepository.getAll();
    }

    @Override
    public void addProject(String name) {
            projectRepository.add(new Project(nextProjectId++, name));
    }

    @Override
    public void deleteProject(int index) {
        projectRepository.remove(index);
    }

    @Override
    public List<Project> getProjects() {
        return projectRepository.getAll();
    }
}
