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
 * Единица техники (камера, объектив, вспышка, штатив, свет).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assets")
public class Asset {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Инвентарный номер. Обязательное, уникальное.
     */
    @Column(name = "inventory_number", nullable = false, unique = true)
    private String inventoryNumber;

    /**
     * Модель. Обязательное.
     */
    @Column(nullable = false)
    private String model;

    /**
     * Категория. Обязательное.
     */
    @Column(nullable = false)
    private String category;

    /**
     * {@code true} — техника на съёмке, {@code false} — в стойке. Обязательное.
     */
    @Column(nullable = false)
    private boolean assigned = false;
}
