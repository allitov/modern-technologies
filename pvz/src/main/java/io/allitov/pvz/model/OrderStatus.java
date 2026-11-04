package io.allitov.pvz.model;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Статус заказа в пункте выдачи с русской подписью и CSS-классом для шаблонов.
 *
 * <p>Каждая константа несёт только отображаемые данные: подпись для интерфейса
 * и css-класс строки таблицы. Английские ключи базы данных
 * ({@code ready}, {@code issued}, {@code cancelled}) совпадают с именами
 * констант без учёта регистра и переводятся в константы методом {@link #fromDb(String)}.</p>
 */
@Getter
@RequiredArgsConstructor
public enum OrderStatus {

    /**
     * Заказ принят и ждёт выдачи получателю.
     */
    READY("К выдаче", "status-ready"),

    /**
     * Заказ выдан получателю.
     */
    ISSUED("Выдан", "status-issued"),

    /**
     * Заказ отменён (не был выдан).
     */
    CANCELLED("Отменён", "status-cancelled");

    /**
     * Русская подпись статуса для отображения в интерфейсе.
     */
    private final String displayName;

    /**
     * CSS-класс для оформления строки таблицы по статусу.
     */
    private final String cssClass;

    /**
     * Преобразует английский ключ статуса из базы данных в константу перечисления.
     *
     * <p>Сравнение выполняется без учёта регистра. Если ключ равен {@code null}
     * или не соответствует ни одной константе, возвращается {@link #READY} —
     * подпись «К выдаче» и css-класс {@code status-ready} используются как
     * безопасное значение по умолчанию, чтобы шаблоны всегда получали корректную
     * подпись и класс строки. Исключение при этом не бросается.</p>
     *
     * @param dbKey английский ключ статуса из базы данных, например {@code "ready"}.
     * @return константа перечисления для заданного ключа; для неизвестного
     *         или пустого значения — {@link #READY}.
     */
    public static OrderStatus fromDb(String dbKey) {
        for (OrderStatus status : values()) {
            if (status.name().equalsIgnoreCase(dbKey)) {
                return status;
            }
        }
        return READY;
    }
}
