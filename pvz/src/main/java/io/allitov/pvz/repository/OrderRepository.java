package io.allitov.pvz.repository;

import io.allitov.pvz.model.Order;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.util.Optional;

@RegisterConstructorMapper(Order.class)
public interface OrderRepository {

    @SqlQuery("select * from orders where order_code = :code;")
    Optional<Order> findByCode(@Bind("code") String code);

    @SqlUpdate("""
    insert into orders (order_code, phone, parcel_article, cell)
    values (:tempCode, :phone, :parcelArticle, :cell);
    """)
    @GetGeneratedKeys("id")
    long insert(@Bind("tempCode") String tempCode, @Bind("phone") String phone,
            @Bind("parcelArticle") String parcelArticle, @Bind("cell") String cell);

    @SqlUpdate("update orders set order_code = :code where id = :id")
    void updateCode(@Bind("id") long id, @Bind("code") String code);

}
