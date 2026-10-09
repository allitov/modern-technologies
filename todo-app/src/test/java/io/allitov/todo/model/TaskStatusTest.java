package io.allitov.todo.model;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

class TaskStatusTest {

    @ParameterizedTest
    @CsvSource({
            "TODO, Todo",
            "IN_PROGRESS, In Progress",
            "DONE, Done"
    })
    void statusReturnsDisplayName(String statusName, String expectedDisplayName) {
        TaskStatus status = TaskStatus.valueOf(statusName);

        assertSoftly(softly -> {
            softly.assertThat(status.getDisplayName()).isEqualTo(expectedDisplayName);
            softly.assertThat(status).hasToString(expectedDisplayName);
        });
    }
}
