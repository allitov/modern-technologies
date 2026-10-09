package io.allitov.todo.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class TaskPriorityTest {

    @ParameterizedTest
    @CsvSource({
            "LOW, Low",
            "MEDIUM, Medium",
            "HIGH, High"
    })
    void priorityReturnsDisplayName(String priorityName, String expectedDisplayName) {
        TaskPriority priority = TaskPriority.valueOf(priorityName);

        assertSoftly(softly -> {
            softly.assertThat(priority.getDisplayName()).isEqualTo(expectedDisplayName);
            softly.assertThat(priority).hasToString(expectedDisplayName);
        });
    }
}
