package io.allitov.cadr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Фотограф студии.
 * Сущность соответствует строке таблицы {@code employees}.</p>
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "employees")
public class Employee {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * ФИО. Обязательное.
     */
    @Column(name = "full_name", nullable = false)
    private String fullName;

    /**
     * Email. Обязательное, уникальное.
     */
    @Column(nullable = false, unique = true)
    private String email;

    /**
     * {@code true} — в штате, {@code false} — уволен. Обязательное.
     */
    @Column(nullable = false)
    private boolean active = true;
}
