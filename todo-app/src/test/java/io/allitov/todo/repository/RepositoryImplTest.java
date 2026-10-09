package io.allitov.todo.repository;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class RepositoryImplTest {

    private RepositoryImpl<String> repository;

    @BeforeEach
    void setUp() {
        repository = new RepositoryImpl<>();
    }

    @Test
    void addStoresItemsInOrder() {
        repository.add("first");
        repository.add("second");

        assertSoftly(softly -> {
            softly.assertThat(repository.size()).isEqualTo(2);
            softly.assertThat(repository.getAll()).containsExactly("first", "second");
        });
    }

    @Test
    void getAllReturnsIndependentCopy() {
        repository.add("first");

        List<String> allItems = repository.getAll();
        allItems.add("modified");

        assertThat(repository.getAll()).containsExactly("first");
    }

    @Test
    void updateReplacesOnlyRequestedItem() {
        repository.add("first");
        repository.add("second");

        repository.update(1, "updated");

        assertThat(repository.getAll()).containsExactly("first", "updated");
    }

    @Test
    void removeDeletesOnlyRequestedItem() {
        repository.add("first");
        repository.add("second");
        repository.add("third");

        repository.remove(1);

        assertThat(repository.getAll()).containsExactly("first", "third");
    }

    @Test
    void clearDeletesAllItems() {
        repository.add("first");
        repository.add("second");

        repository.clear();

        assertThat(repository.size()).isZero();
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void updateRejectsInvalidIndex(int index) {
        assertThatThrownBy(() -> repository.update(index, "updated"))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = {-1, 0})
    void removeRejectsInvalidIndex(int index) {
        assertThatThrownBy(() -> repository.remove(index))
                .isInstanceOf(IndexOutOfBoundsException.class);
    }
}
