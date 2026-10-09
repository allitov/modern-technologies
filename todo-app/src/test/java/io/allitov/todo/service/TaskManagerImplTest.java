package io.allitov.todo.service;

import io.allitov.todo.model.Project;
import io.allitov.todo.model.Task;
import io.allitov.todo.model.TaskPriority;
import io.allitov.todo.model.TaskStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.stream.Stream;
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
        taskManager.addTask("Read a book", "Technical literature", TaskPriority.LOW, TaskStatus.TODO, 0);
        taskManager.addTask("Write a report", "Annual statistics", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, 0);

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
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

        List<Task> tasks = taskManager.getTasks();
        tasks.clear();

        assertThat(taskManager.getTasks()).hasSize(1);
    }

    @Test
    void updateTaskKeepsTaskIdentifierAndChangesValues() {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

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
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);
        taskManager.addTask("Write a report", "", TaskPriority.HIGH, TaskStatus.IN_PROGRESS, 0);

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
    void addTaskAssignsProjectIdentifier() {
        taskManager.addProject("Work");
        int projectId = taskManager.getProjects().getFirst().getId();

        taskManager.addTask("Read a book", "Technical literature", TaskPriority.LOW, TaskStatus.TODO, projectId);

        Task task = taskManager.getTasks().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(task.getProjectId()).isEqualTo(projectId);
            softly.assertThat(task.getDescription()).isEqualTo("Technical literature");
            softly.assertThat(task.getPriority()).isEqualTo(TaskPriority.LOW);
            softly.assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
        });
    }

    @Test
    void moveTaskToProjectChangesOnlyProjectIdentifier() {
        taskManager.addProject("Work");
        int projectId = taskManager.getProjects().getFirst().getId();
        taskManager.addTask("Read a book", "Technical literature", TaskPriority.LOW, TaskStatus.TODO, 0);

        boolean moved = taskManager.moveTaskToProject(0, projectId);

        Task task = taskManager.getTasks().getFirst();
        assertSoftly(softly -> {
            softly.assertThat(moved).isTrue();
            softly.assertThat(task.getProjectId()).isEqualTo(projectId);
            softly.assertThat(task.getId()).isEqualTo(1);
            softly.assertThat(task.getTitle()).isEqualTo("Read a book");
            softly.assertThat(task.getDescription()).isEqualTo("Technical literature");
            softly.assertThat(task.getPriority()).isEqualTo(TaskPriority.LOW);
            softly.assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            softly.assertThat(taskManager.getTasks()).hasSize(1);
            softly.assertThat(taskManager.getProjects()).hasSize(1);
        });
    }

    @Test
    void moveTaskToProjectWithZeroUnbindsTask() {
        taskManager.addProject("Work");
        int projectId = taskManager.getProjects().getFirst().getId();
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, projectId);

        boolean moved = taskManager.moveTaskToProject(0, 0);

        assertSoftly(softly -> {
            softly.assertThat(moved).isTrue();
            softly.assertThat(taskManager.getTasks().getFirst().getProjectId()).isZero();
        });
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 1})
    void moveTaskToProjectRejectsInvalidTaskIndex(int index) {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

        assertThat(taskManager.moveTaskToProject(index, 0)).isFalse();
    }

    @Test
    void moveTaskToProjectRejectsUnknownProject() {
        taskManager.addTask("Read a book", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

        boolean moved = taskManager.moveTaskToProject(0, 42);

        assertSoftly(softly -> {
            softly.assertThat(moved).isFalse();
            softly.assertThat(taskManager.getTasks().getFirst().getProjectId()).isZero();
        });
    }

    @Test
    void countTasksInProjectCountsOnlyProjectTasks() {
        taskManager.addProject("Work");
        taskManager.addProject("Personal");
        int workId = taskManager.getProjects().get(0).getId();
        int personalId = taskManager.getProjects().get(1).getId();
        taskManager.addTask("First", "", TaskPriority.MEDIUM, TaskStatus.TODO, workId);
        taskManager.addTask("Second", "", TaskPriority.MEDIUM, TaskStatus.TODO, workId);
        taskManager.addTask("Third", "", TaskPriority.MEDIUM, TaskStatus.TODO, personalId);
        taskManager.addTask("Unbound", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

        assertSoftly(softly -> {
            softly.assertThat(taskManager.countTasksInProject(workId)).isEqualTo(2);
            softly.assertThat(taskManager.countTasksInProject(personalId)).isEqualTo(1);
            softly.assertThat(taskManager.countTasksInProject(0)).isEqualTo(1);
            softly.assertThat(taskManager.countTasksInProject(999)).isZero();
        });
    }

    @Test
    void countTasksInProjectByStatusCountsOnlyMatchingTasks() {
        taskManager.addProject("Work");
        int projectId = taskManager.getProjects().getFirst().getId();
        taskManager.addTask("First", "", TaskPriority.MEDIUM, TaskStatus.TODO, projectId);
        taskManager.addTask("Second", "", TaskPriority.MEDIUM, TaskStatus.DONE, projectId);
        taskManager.addTask("Third", "", TaskPriority.MEDIUM, TaskStatus.DONE, projectId);
        taskManager.addTask("Other", "", TaskPriority.MEDIUM, TaskStatus.DONE, 0);

        assertSoftly(softly -> {
            softly.assertThat(taskManager.countTasksInProjectByStatus(projectId, TaskStatus.TODO)).isEqualTo(1);
            softly.assertThat(taskManager.countTasksInProjectByStatus(projectId, TaskStatus.DONE)).isEqualTo(2);
            softly.assertThat(taskManager.countTasksInProjectByStatus(projectId, TaskStatus.IN_PROGRESS)).isZero();
        });
    }

    @ParameterizedTest
    @MethodSource("completionPercentArguments")
    void completionPercentReturnsDoneShareOfProjectTasks(List<TaskStatus> statuses, double expectedPercent) {
        taskManager.addProject("Work");
        int projectId = taskManager.getProjects().getFirst().getId();
        for (int i = 0; i < statuses.size(); i++) {
            taskManager.addTask("Task %d".formatted(i + 1), "", TaskPriority.MEDIUM, statuses.get(i), projectId);
        }

        assertThat(taskManager.completionPercent(projectId)).isEqualTo(expectedPercent);
    }

    static Stream<Arguments> completionPercentArguments() {
        return Stream.of(
                Arguments.of(List.of(), 0.0),
                Arguments.of(List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS), 0.0),
                Arguments.of(List.of(TaskStatus.TODO, TaskStatus.DONE), 50.0),
                Arguments.of(List.of(TaskStatus.DONE, TaskStatus.DONE, TaskStatus.DONE), 100.0));
    }

    @Test
    void deleteProjectRemovesOnlyItsTasksAndProject() {
        taskManager.addProject("Work");
        taskManager.addProject("Personal");
        int workId = taskManager.getProjects().get(0).getId();
        int personalId = taskManager.getProjects().get(1).getId();
        taskManager.addTask("Work task", "", TaskPriority.MEDIUM, TaskStatus.TODO, workId);
        taskManager.addTask("Personal task", "", TaskPriority.MEDIUM, TaskStatus.TODO, personalId);
        taskManager.addTask("Unbound task", "", TaskPriority.MEDIUM, TaskStatus.TODO, 0);

        taskManager.deleteProject(0);

        assertSoftly(softly -> {
            softly.assertThat(taskManager.getProjects()).hasSize(1);
            softly.assertThat(taskManager.getProjects().getFirst().getName()).isEqualTo("Personal");
            softly.assertThat(taskManager.getTasks()).hasSize(2);
            softly.assertThat(taskManager.getTasks())
                    .extracting(Task::getTitle)
                    .containsExactly("Personal task", "Unbound task");
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
