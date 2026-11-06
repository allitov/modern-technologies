package io.allitov.cadr.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Выдача техники фотографу на съёмку.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "assignments")
public class Assignment {

    /**
     * Первичный ключ.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Ссылка на выданную единицу техники (внешний ключ на {@code assets.id}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "asset_id", nullable = false)
    private Asset asset;

    /**
     * Ссылка на фотографа, получившего технику (внешний ключ на {@code employees.id}).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "employee_id", nullable = false)
    private Employee employee;

    /**
     * Дата выдачи (unix time). Обязательное, колонка {@code issued_at}.
     */
    @Column(name = "issued_at", nullable = false)
    private long issuedAt;

    /**
     * Дата возврата (unix time). Необязательное: у активной выдачи ещё нет даты возврата.
     */
    @Column(name = "returned_at")
    private Long returnedAt;

    /**
     * Выдача активна, пока дата возврата не проставлена.
     */
    public boolean isActive() {
        return returnedAt == null;
    }
}
