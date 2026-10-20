package io.allitov.pvz.db;

import javax.sql.DataSource;

import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Конфигурация доступа к базе данных через JDBI.
 */
@Configuration
public class JdbiConfig {

    /**
     * Создаёт экземпляр Jdbi на основе источника данных приложения.
     *
     * @param ds источник данных приложения.
     * @return настроенный экземпляр Jdbi.
     */
    @Bean
    public Jdbi jdbi(DataSource ds) {
        return Jdbi.create(ds);
    }
}
