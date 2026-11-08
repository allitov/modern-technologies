package io.allitov.cadr.domain;

/**
 * Доменная ошибка — нарушение бизнес-правила учёта техники.
 */
public class DomainError extends RuntimeException {

    public DomainError(String message) {
        super(message);
    }
}
