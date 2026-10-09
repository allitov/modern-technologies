package io.allitov.todo.repository;

import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Реализация {@link Repository}.
 *
 * @param <T> тип хранимого элемента.
 */
@NoArgsConstructor
public class RepositoryImpl<T> implements Repository<T> {

    private final List<T> items = new ArrayList<>();

    @Override
    public void add(T item) {
        items.add(item);
    }

    @Override
    public void remove(int index) {
        items.remove(index);
    }

    @Override
    public void update(int index, T item) {
        items.set(index, item);
    }

    @Override
    public int size() {
        return items.size();
    }

    @Override
    public List<T> getAll() {
        return new ArrayList<>(items);
    }

    @Override
    public void clear() {
        items.clear();
    }
}
