package io.allitov.pvz.repository;

import io.allitov.pvz.model.Order;
import org.jdbi.v3.sqlobject.config.RegisterConstructorMapper;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindList;
import org.jdbi.v3.sqlobject.statement.GetGeneratedKeys;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@RegisterConstructorMapper(Order.class)
public interface OrderRepository {

    String ORDER_COLUMNS = """
            select
                id,
                order_code as code,
                coalesce(phone, '') as phone,
                coalesce(parcel_article, '') as article,
                coalesce(cell, '') as cell,
                status,
                created_at as createdAt
            """;

    @SqlQuery(ORDER_COLUMNS + " from orders order by created_at desc, id desc")
    List<Order> findAll();

    @SqlQuery(ORDER_COLUMNS + " from orders where order_code ilike :search "
            + "or phone ilike :search or parcel_article ilike :search "
            + "order by created_at desc, id desc")
    List<Order> findBySearchTerm(@Bind("search") String search);

    @SqlQuery(ORDER_COLUMNS + " from orders where status = :status order by created_at desc, id desc")
    List<Order> findByStatus(@Bind("status") String status);

    @SqlQuery(ORDER_COLUMNS + " from orders where order_code = :code")
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

    /**
     * Обновляет статус и дату выдачи заказа по его коду.
     *
     * @param code     код заказа
     * @param status   новый статус (ключ БД, нижний регистр)
     * @param issuedAt дата выдачи
     * @return число обновлённых строк (0 или 1)
     */
    @SqlUpdate("update orders set status = :status, issued_at = :issuedAt where order_code = :code")
    int updateStatusAndIssuedAt(@Bind("code") String code, @Bind("status") String status,
            @Bind("issuedAt") Instant issuedAt);

    /**
     * Обновляет только статус заказа по его коду; остальные колонки не изменяются.
     *
     * @param code   код заказа
     * @param status новый статус (ключ БД, нижний регистр)
     * @return число обновлённых строк (0 или 1)
     */
    @SqlUpdate("update orders set status = :status where order_code = :code")
    int updateStatus(@Bind("code") String code, @Bind("status") String status);

    @SqlUpdate("delete from orders where status in (<statuses>)")
    int deleteByStatuses(@BindList("statuses") Collection<String> statuses);

    @SqlQuery("select count(*) from orders where status in (<statuses>)")
    int countByStatuses(@BindList("statuses") Collection<String> statuses);

}
