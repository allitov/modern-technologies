package io.allitov.pvz.config;

import io.allitov.pvz.repository.OrderRepository;
import org.jdbi.v3.core.Jdbi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JdbiRepositoryConfig {

    @Bean
    OrderRepository orderRepository(Jdbi jdbi) {
        return jdbi.onDemand(OrderRepository.class);
    }
}
