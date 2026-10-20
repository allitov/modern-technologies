package io.allitov.pvz.model;

import lombok.Builder;
import lombok.Data;

/**
 * Сводный отчёт по заказам пункта выдачи.
 *
 * <p>Содержит количество заказов в каждом из трёх статусов
 * и заполняется по данным таблицы {@code orders}.</p>
 */
@Data
@Builder
public class Report {

    /**
     * Количество заказов в статусе {@code ready} («К выдаче»).
     */
    private int readyCount;

    /**
     * Количество заказов в статусе {@code issued} («Выдан»).
     */
    private int issuedCount;

    /**
     * Количество заказов в статусе {@code cancelled} («Отменён»).
     */
    private int cancelledCount;
}
