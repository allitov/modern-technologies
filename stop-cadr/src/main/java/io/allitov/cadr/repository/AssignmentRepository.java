package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Assignment;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Доступ к таблице {@code assignments}.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Есть ли у фотографа техника на съёмке: выдача по {@code employee_id},
     * у которой ещё нет даты возврата.
     */
    boolean existsByEmployeeIdAndReturnedAtIsNull(Long employeeId);

    /**
     * Все активные выдачи (дата возврата не проставлена) — по ним определяется
     * текущий держатель каждой единицы техники.
     */
    @EntityGraph(attributePaths = {"asset", "employee"})
    List<Assignment> findByReturnedAtIsNull();

    /**
     * Активная выдача конкретной единицы техники: выдача по {@code asset_id},
     * у которой ещё нет даты возврата.
     */
    @EntityGraph(attributePaths = {"asset", "employee"})
    Optional<Assignment> findByAssetIdAndReturnedAtIsNull(Long assetId);

    /**
     * Все выдачи единицы техники — история выдач ({@code assetHistory}).
     */
    @EntityGraph(attributePaths = {"employee"})
    List<Assignment> findByAssetId(Long assetId);

    /**
     * Активные выдачи фотографа — техника, которая сейчас у него на руках
     * ({@code employeeAssets}).
     */
    @EntityGraph(attributePaths = {"asset"})
    List<Assignment> findByEmployeeIdAndReturnedAtIsNull(Long employeeId);

    /**
     * Удалить все выдачи единицы техники — каскад при списании
     * ({@code deleteAssetCascade}).
     */
    void deleteByAssetId(Long assetId);
}
