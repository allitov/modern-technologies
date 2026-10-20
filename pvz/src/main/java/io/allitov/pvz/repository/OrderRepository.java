package io.allitov.pvz.repository;

import io.allitov.pvz.model.Order;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.SqlQuery;

import java.util.Optional;

@RegisterConstructorMapper(Order.class)
public interface OrderRepository {

    @SqlQuery("select * from orders where order_code = :code;")
    Optional<Order> findByCode(@Bind("code") String code);

}
