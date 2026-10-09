package io.allitov.todo.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class TaskTest {

    @Test
    void constructorCreatesTaskWithDefaultValues() {
        Task task = new Task(7, "Read a book");

        assertSoftly(softly -> {
            softly.assertThat(task.getId()).isEqualTo(7);
            softly.assertThat(task.getTitle()).isEqualTo("Read a book");
            softly.assertThat(task.getDescription()).isEmpty();
            softly.assertThat(task.getStatus()).isEqualTo(TaskStatus.TODO);
            softly.assertThat(task.getPriority()).isEqualTo(TaskPriority.MEDIUM);
        });
    }

    @Test
    void settersChangeTaskValues() {
        Task task = new Task(1, "Read a book");

        task.setTitle("Write a report");
        task.setDescription("Use the annual statistics");
        task.setStatus(TaskStatus.IN_PROGRESS);
        task.setPriority(TaskPriority.HIGH);

        assertSoftly(softly -> {
            softly.assertThat(task.getTitle()).isEqualTo("Write a report");
            softly.assertThat(task.getDescription()).isEqualTo("Use the annual statistics");
            softly.assertThat(task.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
            softly.assertThat(task.getPriority()).isEqualTo(TaskPriority.HIGH);
        });
    }

    @Test
    void tasksWithSameValuesAreEqual() {
        Task first = new Task(1, "Read a book");
        Task second = new Task(1, "Read a book");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("Task(id=1, title=Read a book, description=, status=Todo, priority=Medium)");
    }
}
