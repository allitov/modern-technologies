package io.allitov.imgprocessor;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Optional;

/**
 * Режим пакетной обработки изображений.
 */
@Getter
@RequiredArgsConstructor
public enum ProcessingMode {

    SEQUENTIAL(1, "Sequential"),

    PARALLEL(2, "Parallel");

    private final int number;

    private final String label;

    /**
     * Возвращает режим по его номеру в меню.
     *
     * @param number номер режима.
     * @return режим или пустой {@link Optional}, если номер неизвестен.
     */
    public static Optional<ProcessingMode> fromNumber(int number) {
        for (ProcessingMode mode : values()) {
            if (mode.number == number) {
                return Optional.of(mode);
            }
        }

        return Optional.empty();
    }
}
