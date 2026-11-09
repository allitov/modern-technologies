package io.allitov.cadr.exception;

/**
 * Доменная ошибка — нарушение бизнес-правила учёта техники.
 */
public class DomainError extends RuntimeException {

    public DomainError(String message) {
        super(message);
    }
}
