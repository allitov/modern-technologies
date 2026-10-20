package io.allitov.pvz.config;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.postgres.PostgresPlugin;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy;

/**
 * Конфигурация доступа к базе данных через JDBI.
 */
@Configuration
public class JdbiConfig {

    /**
     * Создаёт экземпляр Jdbi на основе источника данных приложения.
     *
     * @param dataSource источник данных приложения.
     * @return настроенный экземпляр Jdbi.
     */
    @Bean
    Jdbi jdbi(DataSource dataSource) {
        TransactionAwareDataSourceProxy proxy = new TransactionAwareDataSourceProxy(dataSource);
        Jdbi jdbi = Jdbi.create(proxy);
        jdbi.installPlugin(new SqlObjectPlugin());
        jdbi.installPlugin(new PostgresPlugin());

        return jdbi;
    }
}
