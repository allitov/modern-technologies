package io.allitov.pvz.service.impl;

import io.allitov.pvz.service.OrderService;
import org.jdbi.v3.core.Jdbi;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers(disabledWithoutDocker = true)
class OrderServiceImplIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer postgres = new PostgreSQLContainer("postgres:16-alpine");

    @Autowired
    private OrderService orderService;

    @Autowired
    private Jdbi jdbi;

    @BeforeEach
    void truncateOrders() {
        jdbi.useHandle(handle -> handle.execute("truncate table orders restart identity"));
    }

    @Nested
    class AddOrder {

        @Test
        @DisplayName("Заказ по телефону с ячейкой сохраняется со статусом ready и кодом PVZ-000001")
        void acceptsPhoneOrderWithCellAsReadyWithCodePvz000001() throws SQLException {
            String code = orderService.addOrder("+79991234567", "A-01");

            assertThat(code).isEqualTo("PVZ-000001");
            OrderRow row = fetchByCodeFromSeparateConnection(code);
            assertThat(row.status()).isEqualTo("ready");
            assertThat(row.phone()).isEqualTo("+79991234567");
            assertThat(row.parcelArticle()).isNull();
            assertThat(row.cell()).isEqualTo("A-01");
        }

        @Test
        @DisplayName("Заказ по артикулу без ячейки получает код PVZ-000002, телефон и ячейка пусты")
        void acceptsArticleOrderWithoutCellAsSecondWithCodePvz000002() throws SQLException {
            orderService.addOrder("+79991234567", "A-01");

            String code = orderService.addOrder("ART-123", null);

            assertThat(code).isEqualTo("PVZ-000002");
            OrderRow row = fetchByCodeFromSeparateConnection(code);
            assertThat(row.status()).isEqualTo("ready");
            assertThat(row.phone()).isNull();
            assertThat(row.parcelArticle()).isEqualTo("ART-123");
            assertThat(row.cell()).isNull();
        }

        @Test
        @DisplayName("Заказ закоммичен и виден из отдельного соединения с базой")
        void commitsOrderVisibleFromSeparateConnection() throws SQLException {
            String code = orderService.addOrder("ART-123", "B-02");

            assertThat(fetchByCodeFromSeparateConnection(code).parcelArticle()).isEqualTo("ART-123");
        }

        @Test
        @DisplayName("Два заказа на один телефон получают разные уникальные коды")
        void assignsUniqueCodesToTwoOrdersWithSamePhone() throws SQLException {
            String first = orderService.addOrder("+79991234567", "A-01");
            String second = orderService.addOrder("+79991234567", "A-02");

            assertThat(first).isEqualTo("PVZ-000001");
            assertThat(second).isEqualTo("PVZ-000002").isNotEqualTo(first);
            assertThat(fetchByCodeFromSeparateConnection(first).cell()).isEqualTo("A-01");
            assertThat(fetchByCodeFromSeparateConnection(second).cell()).isEqualTo("A-02");
        }

        private OrderRow fetchByCodeFromSeparateConnection(String code) throws SQLException {
            try (Connection connection = DriverManager.getConnection(
                    postgres.getJdbcUrl(), postgres.getUsername(), postgres.getPassword());
                 PreparedStatement statement = connection.prepareStatement(
                         "select status, phone, parcel_article, cell from orders where order_code = ?")) {
                statement.setString(1, code);
                try (ResultSet resultSet = statement.executeQuery()) {
                    assertThat(resultSet.next()).isTrue();
                    return new OrderRow(resultSet.getString("status"), resultSet.getString("phone"),
                            resultSet.getString("parcel_article"), resultSet.getString("cell"));
                }
            }
        }
    }

    private record OrderRow(String status, String phone, String parcelArticle, String cell) {
    }
}
