package io.allitov.pvz.exception;

/**
 * Выбрасывается, когда приёмка заказа не выполнена из-за ошибки базы данных
 * (например, нарушения её ограничений целостности или уникальности).
 *
 * <p>Наследуется от {@link RuntimeException}, поэтому не требует объявления
 * в сигнатурах; контроллер перехватывает его и превращает во flash-сообщение
 * об ошибке вместо HTTP 500.</p>
 */
public class OrderCreationException extends RuntimeException {

    /**
     * Создаёт исключение с сообщением и причиной.
     *
     * @param message описание ошибки на английском.
     * @param cause   исходная ошибка доступа к данным.
     */
    public OrderCreationException(String message, Throwable cause) {
        super(message, cause);
    }
}
