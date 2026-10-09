package io.allitov.todo.service;

import io.allitov.todo.model.Project;
import io.allitov.todo.model.Task;
import io.allitov.todo.model.TaskPriority;
import io.allitov.todo.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class TaskManagerImplTest {

    private TaskManagerImpl taskManager;

    @BeforeEach
    void setUp() {
        taskManager = new TaskManagerImpl();
    }

    @Test
    void addTaskCreatesSequentialTasksWithValues() {
        taskManager.addTask("Read a book", "Technical literature", TaskPriority.LOW, TaskStatus.TODO);
        taskManager.addTask("Write a report", "Annual statistics", TaskPriority.HIGH, TaskStatus.IN_PROGRESS);

        List<Task> tasks = taskManager.getTasks();

        assertSoftly(softly -> {
            softly.assertThat(tasks).hasSize(2);
            softly.assertThat(tasks.get(0).getId()).isEqualTo(1);
            softly.assertThat(tasks.get(1).getId()).isEqualTo(2);
            softly.assertThat(tasks.get(1).getTitle()).isEqualTo("Write a report");
            softly.assertThat(tasks.get(1).getDescription()).isEqualTo("Annual statistics");
            softly.assertThat(tasks.get(1).getPriority()).isEqualTo(TaskPriority.HIGH);
            softly.assertThat(tasks.get(1).getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        });
    }

    @Test
    void getTasksReturnsIndependentListCopy() {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO);

        List<Task> tasks = taskManager.getTasks();
        tasks.clear();

        assertThat(taskManager.getTasks()).hasSize(1);
    }

    @Test
    void updateTaskKeepsTaskIdentifierAndChangesValues() {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO);

        taskManager.updateTask(0, "Write a report", "Annual statistics",
                TaskPriority.HIGH, TaskStatus.IN_PROGRESS);

        Task task = taskManager.getTasks().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(task.getId()).isEqualTo(1);
            softly.assertThat(task.getTitle()).isEqualTo("Write a report");
            softly.assertThat(task.getDescription()).isEqualTo("Annual statistics");
            softly.assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
            softly.assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        });
    }

    @Test
    void deleteTaskRemovesOnlyRequestedTask() {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO);
        taskManager.addTask("Write a report", "", TaskPriority.HIGH, TaskStatus.IN_PROGRESS);

        taskManager.deleteTask(0);

        List<Task> tasks = taskManager.getTasks();
        assertSoftly(softly -> {
            softly.assertThat(tasks).hasSize(1);
            softly.assertThat(tasks.getFirst().getTitle()).isEqualTo("Write a report");
        });
    }

    @Test
    void addProjectCreatesSequentialProjectsWithValues() {
        taskManager.addProject("Work");
        taskManager.addProject("Personal");

        List<Project> projects = taskManager.getProjects();

        assertSoftly(softly -> {
            softly.assertThat(projects).hasSize(2);
            softly.assertThat(projects.get(0).getId()).isEqualTo(1);
            softly.assertThat(projects.get(0).getName()).isEqualTo("Work");
            softly.assertThat(projects.get(1).getId()).isEqualTo(2);
            softly.assertThat(projects.get(1).getName()).isEqualTo("Personal");
        });
    }

    @Test
    void getProjectsReturnsIndependentListCopy() {
        taskManager.addProject("Work");

        List<Project> projects = taskManager.getProjects();
        projects.clear();

        assertThat(taskManager.getProjects()).hasSize(1);
    }

    @Test
    void deleteProjectRemovesOnlyRequestedProject() {
        taskManager.addProject("Work");
        taskManager.addProject("Personal");

        taskManager.deleteProject(1);

        List<Project> projects = taskManager.getProjects();
        assertSoftly(softly -> {
            softly.assertThat(projects).hasSize(1);
            softly.assertThat(projects.getFirst().getName()).isEqualTo("Work");
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void updateTaskRejectsInvalidIndex(int index) {
        assertThatThrownBy(() -> taskManager.updateTask(index, "title", "",
                TaskPriority.MEDIUM, TaskStatus.TODO))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void deleteTaskRejectsInvalidIndex(int index) {
        assertThatThrownBy(() -> taskManager.deleteTask(index))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void deleteProjectRejectsInvalidIndex(int index) {
        assertThatThrownBy(() -> taskManager.deleteProject(index))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
