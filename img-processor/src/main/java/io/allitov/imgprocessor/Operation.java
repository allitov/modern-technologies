package io.allitov.imgprocessor;

import java.util.Optional;

/**
 * Операция обработки изображения.
 */
public enum Operation {

    GRAYSCALE(1, "Grayscale"),

    INVERT(2, "Invert"),

    GRAYSCALE_AND_INVERT(3, "Grayscale + Invert");

    private final int number;

    private final String label;

    Operation(int number, String label) {
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
     * Применяет операцию к изображению.
     *
     * @param image обрабатываемое изображение.
     */
    public void apply(Image image) {
        switch (this) {
            case GRAYSCALE -> image.grayscale();
            case INVERT -> image.invert();
            case GRAYSCALE_AND_INVERT -> {
                image.grayscale();
                image.invert();
            }
        }
    }

    /**
     * Возвращает операцию по её номеру в меню.
     *
     * @param number номер операции.
     * @return операция или пустой {@link Optional}, если номер неизвестен.
     */
    public static Optional<Operation> fromNumber(int number) {
        for (Operation operation : values()) {
            if (operation.number == number) {
                return Optional.of(operation);
            }
        }

        return Optional.empty();
    }
}
