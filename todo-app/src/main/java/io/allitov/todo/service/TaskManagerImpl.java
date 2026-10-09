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
 * Реализация {@link TaskManager}.
 */
@NoArgsConstructor
public class TaskManagerImpl implements TaskManager {

    private final Repository<Task> taskRepository = new RepositoryImpl<>();

    private final Repository<Project> projectRepository = new RepositoryImpl<>();

    private int nextTaskId = 1;

    private int nextProjectId = 1;

    @Override
    public void addTask(String title, String description, TaskPriority priority, TaskStatus status, int projectId) {
        Task task = new Task(nextTaskId++, title, projectId);
        task.setDescription(description);
        task.setPriority(priority);
        task.setStatus(status);
        taskRepository.add(task);
    }

    @Override
    public boolean moveTaskToProject(int taskIndex, int projectId) {
        List<Task> tasks = taskRepository.getAll();
        if (taskIndex < 0 || taskIndex >= tasks.size()) {
            return false;
        }

        if (projectId != 0 && findProjectIndex(projectId) < 0) {
            return false;
        }

        Task task = tasks.get(taskIndex);
        task.setProjectId(projectId);
        taskRepository.update(taskIndex, task);
        return true;
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
        Project project = projectRepository.getAll().get(index);
        List<Task> tasks = taskRepository.getAll();
        for (int i = tasks.size() - 1; i >= 0; i--) {
            if (tasks.get(i).getProjectId() == project.getId()) {
                taskRepository.remove(i);
            }
        }

        projectRepository.remove(index);
    }

    @Override
    public List<Project> getProjects() {
        return projectRepository.getAll();
    }

    @Override
    public int countTasksInProject(int projectId) {
        return (int) taskRepository.getAll().stream()
                .filter(task -> task.getProjectId() == projectId)
                .count();
    }

    @Override
    public int countTasksInProjectByStatus(int projectId, TaskStatus status) {
        return (int) taskRepository.getAll().stream()
                .filter(task -> task.getProjectId() == projectId && task.getStatus() == status)
                .count();
    }

    @Override
    public double completionPercent(int projectId) {
        List<Task> projectTasks = taskRepository.getAll().stream()
                .filter(task -> task.getProjectId() == projectId)
                .toList();
        if (projectTasks.isEmpty()) {
            return 0.0;
        }

        long doneCount = projectTasks.stream()
                .filter(task -> task.getStatus() == TaskStatus.DONE)
                .count();
        return 100.0 * doneCount / projectTasks.size();
    }

    /**
     * Определяет индекс проекта по его идентификатору.
     *
     * @param projectId идентификатор проекта.
     * @return индекс проекта в хранилище или {@code -1}, если проект не найден.
     */
    private int findProjectIndex(int projectId) {
        List<Project> projects = projectRepository.getAll();
        for (int i = 0; i < projects.size(); i++) {
            if (projects.get(i).getId() == projectId) {
                return i;
            }
        }

        return -1;
    }
}
