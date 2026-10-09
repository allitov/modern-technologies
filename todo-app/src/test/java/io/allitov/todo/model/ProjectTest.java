package io.allitov.todo.model;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class ProjectTest {

    @Test
    void noArgConstructorCreatesEmptyProject() {
        Project project = new Project();

        assertSoftly(softly -> {
            softly.assertThat(project.getId()).isZero();
            softly.assertThat(project.getName()).isEmpty();
        });
    }

    @Test
    void allArgConstructorCreatesProjectWithValues() {
        Project project = new Project(7, "Work");

        assertSoftly(softly -> {
            softly.assertThat(project.getId()).isEqualTo(7);
            softly.assertThat(project.getName()).isEqualTo("Work");
        });
    }

    @Test
    void settersChangeProjectValues() {
        Project project = new Project();

        project.setId(3);
        project.setName("Personal");

        assertSoftly(softly -> {
            softly.assertThat(project.getId()).isEqualTo(3);
            softly.assertThat(project.getName()).isEqualTo("Personal");
        });
    }

    @Test
    void projectsWithSameValuesAreEqual() {
        Project first = new Project(1, "Work");
        Project second = new Project(1, "Work");

        assertThat(first)
                .isEqualTo(second)
                .hasSameHashCodeAs(second)
                .hasToString("Project(id=1, name=Work)");
    }
}
