package io.allitov.pvz.service;

import io.allitov.pvz.exception.OrderCreationException;
import io.allitov.pvz.model.Order;
import io.allitov.pvz.model.Report;

import java.util.List;

public interface OrderService {

    boolean issueOrder(String orderCode);

    boolean cancelOrder(String orderCode);

    Report buildReport();

    /**
     * Возвращает заказы, отфильтрованные по подстроке поиска.
     *
     * <p>Пустой фильтр ({@code null}, пустая или состоящая только из пробелов
     * строка) означает «без фильтра»: возвращаются все заказы. Непустой
     * фильтр трактуется как подстрока без учёта регистра, которая ищется
     * в коде заказа, телефоне получателя или артикуле товара. Заказы
     * упорядочены «свежие сверху». Данные не изменяются.</p>
     *
     * @param filter подстрока поиска; допустимы {@code null} и пробельные строки.
     * @return список заказов, упорядоченный по дате приёма по убыванию
     *         (свежие сверху); пустой список, если ничего не найдено.
     */
    List<Order> listOrders(String filter);

    List<Order> listByStatus(String status);

    int countClosedOrders();

    int deleteClosedOrders();

    /**
     * Принимает заказ: определяет по контакту, телефон это или артикул,
     * и сохраняет ровно одно из полей {@code phone} / {@code parcel_article}.
     *
     * <p>Внутри одной транзакции вставляется строка заказа со временным
     * уникальным кодом, из сгенерированного идентификатора формируется
     * финальный код, которым строка обновляется.</p>
     *
     * @param contact непустой контакт длиной не менее 5 символов:
     *                телефон получателя или артикул товара.
     * @param cell    код ячейки хранения; может быть пустым, не длиннее 30 символов.
     * @return сгенерированный код заказа вида {@code PVZ-000042}.
     * @throws OrderCreationException при ошибке базы данных
     *                                (например, нарушении её ограничений).
     */
    String addOrder(String contact, String cell);
}
