package io.allitov.cadr.repository;

import io.allitov.cadr.domain.Assignment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Доступ к таблице {@code assignments}.
 */
public interface AssignmentRepository extends JpaRepository<Assignment, Long> {

    /**
     * Есть ли у фотографа техника на съёмке: выдача по {@code employee_id},
     * у которой ещё нет даты возврата (аналог ODB-запроса с {@code returnedAt_.is_null()}).
     */
    boolean existsByEmployeeIdAndReturnedAtIsNull(Long employeeId);

    /**
     * Все активные выдачи (дата возврата не проставлена) — по ним определяется
     * текущий держатель каждой единицы техники.
     */
    List<Assignment> findByReturnedAtIsNull();
}
