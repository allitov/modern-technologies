package io.allitov.pvz.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.Instant;

/**
 * Заказ, принятый в пункт выдачи (ПВЗ).
 *
 * <p>Сущность соответствует строке таблицы {@code orders}: поле {@link #article}
 * хранится в колонке {@code parcel_article}, поле {@link #status} — английский
 * ключ статуса из базы данных ({@code ready}, {@code issued}, {@code cancelled}).
 * Дата создания {@link #createdAt} представлена строкой в формате
 * {@code yyyy-MM-dd HH:mm} и формируется при маппинге из базы данных.</p>
 */
@Data
@Builder
@AllArgsConstructor
public class Order {

    /**
     * Идентификатор заказа в базе данных.
     */
    private long id;

    /**
     * Артикул заказа, сгенерированный при приёме, например {@code PVZ-000042}.
     */
    private String code;

    /**
     * Телефон получателя; {@code null}, если при приёме был указан артикул.
     */
    private String phone;

    /**
     * Артикул товара (в базе данных — колонка {@code parcel_article});
     * {@code null}, если при приёме был указан телефон.
     */
    private String article;

    /**
     * Номер ячейки хранения; может быть пустым.
     */
    private String cell;

    /**
     * Английский ключ статуса заказа: {@code ready}, {@code issued} или {@code cancelled}.
     */
    private String status;

    /**
     * Дата и время приёма заказа в формате {@code yyyy-MM-dd HH:mm}.
     */
    private Instant createdAt;
}
