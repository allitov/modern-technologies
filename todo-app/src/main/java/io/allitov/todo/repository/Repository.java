package io.allitov.todo.repository;

import java.util.List;

/**
 * Универсальное хранилище элементов в памяти.
 *
 * @param <T> тип хранимого элемента.
 */
public interface Repository<T> {

    /**
     * Добавляет элемент в конец хранилища.
     *
     * @param item добавляемый элемент.
     */
    void add(T item);

    /**
     * Удаляет элемент по его индексу.
     *
     * @param index индекс удаляемого элемента.
     */
    void remove(int index);

    /**
     * Заменяет элемент по его индексу.
     *
     * @param index индекс заменяемого элемента.
     * @param item новый элемент.
     */
    void update(int index, T item);

    /**
     * Возвращает количество элементов в хранилище.
     *
     * @return количество элементов.
     */
    int size();

    /**
     * Возвращает копию списка всех элементов хранилища.
     *
     * @return список всех элементов.
     */
    List<T> getAll();

    /**
     * Удаляет все элементы из хранилища.
     */
    void clear();
}
