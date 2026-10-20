package io.allitov.pvz.db;

import java.sql.ResultSet;

import java.sql.SQLException;

import java.time.format.DateTimeFormatter;

import java.util.List;

import java.util.Optional;

import org.jdbi.v3.core.Handle;

import org.jdbi.v3.core.Jdbi;

import org.jdbi.v3.core.mapper.RowMapper;

import org.jdbi.v3.core.statement.Query;

import org.jdbi.v3.core.statement.StatementContext;

import org.springframework.stereotype.Repository;

import io.allitov.pvz.model.Order;

import io.allitov.pvz.model.Report;

import lombok.RequiredArgsConstructor;

/**
 * Реализация слоя доступа к базе данных пункта выдачи на JDBI.
 *
 * <p>Каждый метод выполняет параметризованные SQL-запросы: все значения
 * передаются bind-параметрами, конкатенация пользовательского ввода в SQL
 * не допускается. Многошаговые операции ({@link #addOrder}, {@link #issueOrder},
 * {@link #cancelOrder}) выполняются в одной транзакции, поэтому промежуточные
 * состояния не видны другим подключениям.</p>
 */
@Repository
@RequiredArgsConstructor
public class DatabaseImpl implements Database {

    /**
     * Базовый SELECT заказа: колонки таблицы {@code orders} в порядке полей модели.
     */
    private static final String SELECT_ORDERS =
        "SELECT id, order_code, phone, parcel_article, cell, status, created_at FROM orders";

    /**
     * Сортировка списка заказов: свежие сверху.
     */
    private static final String ORDER_BY_RECENT_FIRST = " ORDER BY created_at DESC, id DESC";

    /**
     * Формат отображения даты приёма заказа.
     */
    private static final DateTimeFormatter CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final Jdbi jdbi;

    @Override
    public List<Order> listOrders(String filter) {
        boolean hasFilter = filter != null && !filter.isEmpty();
        String condition = hasFilter
            ? " WHERE (order_code ILIKE :p OR phone ILIKE :p OR parcel_article ILIKE :p)"
            : "";
        return jdbi.withHandle(handle ->
            selectOrders(handle, condition, hasFilter ? "p" : null, hasFilter ? "%" + filter + "%" : null));
    }

    @Override
    public String addOrder(String contact, String cell) {
        return jdbi.inTransaction(handle -> {
            String insert = looksLikePhone(contact)
                ? "INSERT INTO orders (phone, cell) VALUES (:contact, :cell) RETURNING id"
                : "INSERT INTO orders (parcel_article, cell) VALUES (:contact, :cell) RETURNING id";
            long id = handle.createQuery(insert)
                .bind("contact", contact)
                .bind("cell", cell == null || cell.isEmpty() ? null : cell)
                .mapTo(Long.class)
                .one();
            String code = String.format("PVZ-%06d", id);
            handle.createUpdate("UPDATE orders SET order_code = :code WHERE id = :id")
                .bind("code", code)
                .bind("id", id)
                .execute();
            return code;
        });
    }

    @Override
    public boolean issueOrder(String code) {
        return transitionReadyOrder(code, "issued", true);
    }

    @Override
    public boolean cancelOrder(String code) {
        return transitionReadyOrder(code, "cancelled", false);
    }

    @Override
    public Report buildReport() {
        return jdbi.withHandle(handle ->
            handle.createQuery(
                    "SELECT COUNT(*) FILTER (WHERE status = 'ready') AS ready_count, "
                        + "COUNT(*) FILTER (WHERE status = 'issued') AS issued_count, "
                        + "COUNT(*) FILTER (WHERE status = 'cancelled') AS cancelled_count FROM orders")
                .map((rs, ctx) -> Report.builder()
                    .readyCount(rs.getInt("ready_count"))
                    .issuedCount(rs.getInt("issued_count"))
                    .cancelledCount(rs.getInt("cancelled_count"))
                    .build())
                .first());
    }

    @Override
    public List<Order> listByStatus(String status) {
        boolean hasStatus = status != null && !status.isEmpty();
        String condition = hasStatus ? " WHERE status = :status" : "";
        return jdbi.withHandle(handle ->
            selectOrders(handle, condition, hasStatus ? "status" : null, status));
    }

    @Override
    public int countClosedOrders() {
        return jdbi.withHandle(handle ->
                handle.createQuery("SELECT COUNT(*) FROM orders WHERE status IN ('issued', 'cancelled')")
                    .mapTo(Long.class)
                    .one())
            .intValue();
    }

    @Override
    public int deleteClosedOrders() {
        return jdbi.withHandle(handle ->
            handle.createUpdate("DELETE FROM orders WHERE status IN ('issued', 'cancelled')")
                .execute());
    }

    /**
     * Переводит заказ из статуса {@code ready} в целевой в одной транзакции.
     *
     * <p>Атомарность обеспечивает транзакция: сначала читается текущий статус
     * по коду заказа; если строки нет или статус не {@code ready}, транзакция
     * завершается без изменений и возвращается {@code false}. Иначе выполняется
     * UPDATE статуса. Поле {@code issued_at} заполняется только при выдаче,
     * ячейка не изменяется.</p>
     *
     * @param code код заказа, например {@code PVZ-000042}.
     * @param newStatus целевой статус: {@code issued} или {@code cancelled}.
     * @param setIssuedAt заполнять ли поле {@code issued_at} текущим временем.
     * @return {@code true}, если перевод выполнен; {@code false}, если заказ не найден или не в статусе {@code ready}.
     */
    private boolean transitionReadyOrder(String code, String newStatus, boolean setIssuedAt) {
        return jdbi.inTransaction(handle -> {
            Optional<String> current = handle.createQuery("SELECT status FROM orders WHERE order_code = :code")
                .bind("code", code)
                .mapTo(String.class)
                .findFirst();
            if (current.isEmpty() || !"ready".equals(current.get())) {
                return false;
            }
            String update = setIssuedAt
                ? "UPDATE orders SET status = :status, issued_at = NOW() WHERE order_code = :code"
                : "UPDATE orders SET status = :status WHERE order_code = :code";
            handle.createUpdate(update)
                .bind("status", newStatus)
                .bind("code", code)
                .execute();
            return true;
        });
    }

    /**
     * Выполняет базовый SELECT заказов с необязательным условием.
     *
     * <p>Условие передаётся готовым фрагментом SQL с bind-параметром
     * {@code paramName}; пользовательский ввод попадает в запрос только
     * через bind-значение {@code paramValue}.</p>
     *
     * @param handle открытый JDBI-дескриптор соединения.
     * @param condition фрагмент WHERE или пустая строка для выборки всех заказов.
     * @param paramName имя bind-параметра или {@code null}, если условия нет.
     * @param paramValue значение bind-параметра; игнорируется при {@code paramName == null}.
     * @return список заказов, отсортированный от свежих к старым.
     */
    private List<Order> selectOrders(Handle handle, String condition, String paramName, String paramValue) {
        Query query = handle.createQuery(SELECT_ORDERS + condition + ORDER_BY_RECENT_FIRST);
        if (paramName != null) {
            query.bind(paramName, paramValue);
        }
        return query.map(new OrderRowMapper()).list();
    }

    /**
     * Проверяет, похожа ли строка контакта на номер телефона.
     *
     * <p>Порт функции {@code looksLikePhone} из референсной реализации
     * {@code si/db.cpp:9-19}: пустая строка телефоном не считается; разрешены
     * только ASCII-цифры и символы {@code +}, {@code -}, пробел, круглые скобки;
     * любой другой символ запрещает телефон; итоговое условие — не менее 10 цифр.</p>
     *
     * @param contact строка контакта из формы приёмки (телефон или артикул товара).
     * @return {@code true}, если строка похожа на номер телефона.
     */
    private static boolean looksLikePhone(String contact) {
        if (contact.isEmpty()) {
            return false;
        }
        int digits = 0;
        for (int i = 0; i < contact.length(); i++) {
            char c = contact.charAt(i);
            if (c >= '0' && c <= '9') {
                digits++;
                continue;
            }
            if (c == '+' || c == '-' || c == ' ' || c == '(' || c == ')') {
                continue;
            }
            return false;
        }
        return digits >= 10;
    }

    /**
     * Преобразует строку результата запроса в модель {@link Order}.
     *
     * <p>NULL-значения колонок {@code phone}, {@code parcel_article} и {@code cell}
     * заменяются пустой строкой; артикул читается из колонки {@code parcel_article};
     * {@code created_at} (TIMESTAMP) форматируется в {@code yyyy-MM-dd HH:mm};
     * статус переносится как есть — английский ключ базы данных.</p>
     */
    private static class OrderRowMapper implements RowMapper<Order> {

        @Override
        public Order map(ResultSet rs, StatementContext ctx) throws SQLException {
            return Order.builder()
                .id(rs.getInt("id"))
                .code(rs.getString("order_code"))
                .phone(nullableToEmpty(rs.getString("phone")))
                .article(nullableToEmpty(rs.getString("parcel_article")))
                .cell(nullableToEmpty(rs.getString("cell")))
                .status(rs.getString("status"))
                .createdAt(rs.getTimestamp("created_at").toLocalDateTime().format(CREATED_AT_FORMAT))
                .build();
        }

        /**
         * Возвращает пустую строку вместо {@code null}-значения колонки.
         *
         * @param value значение колонки из результата запроса.
         * @return исходное значение или {@code ""}, если оно {@code null}.
         */
        private static String nullableToEmpty(String value) {
            return value == null ? "" : value;
        }
    }
}
