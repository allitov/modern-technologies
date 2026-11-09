package io.allitov.imgprocessor;

import java.util.Optional;

/**
 * Режим пакетной обработки изображений.
 */
public enum ProcessingMode {

    SEQUENTIAL(1, "Sequential"),

    PARALLEL(2, "Parallel");

    private final int number;

    private final String label;

    ProcessingMode(int number, String label) {
        this.number = number;
        this.label = label;
    }

    public int getNumber() {
        return number;
    }

    public String getLabel() {
        return label;
    }

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
