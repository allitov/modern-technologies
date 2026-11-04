package io.allitov.pvz.model;

import lombok.Builder;
import lombok.Value;

/**
 * Критерий выборки заказов для списка ({@code listOrders}).
 *
 * <p>Поле {@link #search} задаёт подстроку поиска; {@code null} означает,
 * что фильтр не задан и метод возвращает все заказы.</p>
 *
 * <p>Неизменяемый ({@code @Value}) и собираемый через {@code @Builder}:
 * новые критерии (статусы, пагинация) добавляются полями без ломки вызывателей.</p>
 */
@Value
@Builder
public class OrderFilter {

    /** подстрока поиска; {@code null} — фильтр не задан (вернуть все заказы). */
    String search;

    /** нормализует вход: null/blank → null (без фильтра), иначе trim. */
    public static OrderFilter of(String search) {
        return new OrderFilter(search == null || search.isBlank() ? null : search.trim());
    }
}
