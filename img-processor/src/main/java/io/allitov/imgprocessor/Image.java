package io.allitov.imgprocessor;

import lombok.Getter;
import lombok.NoArgsConstructor;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

/**
 * Класс для работы с одним изображением.
 * Все операции с многопоточностью находятся вне этого класса.
 */
@NoArgsConstructor
public class Image {

    /**
     * Изображение хранится в формате RGBA.
     */
    private static final int CHANNELS = 4;

    @Getter
    private int width = 0;

    @Getter
    private int height = 0;

    private int channels = CHANNELS;

    private byte[] pixels = new byte[0];

    /**
     * Загружает изображение из файла и приводит его к формату RGBA.
     *
     * @param filename путь к файлу изображения.
     * @return {@code true}, если изображение успешно загружено.
     */
    public boolean load(String filename) {
        BufferedImage source;
        try {
            source = ImageIO.read(new File(filename));
        } catch (IOException _) {
            source = null;
        }

        if (source == null) {
            width = 0;
            height = 0;
            pixels = new byte[0];
            return false;
        }

        width = source.getWidth();
        height = source.getHeight();
        channels = CHANNELS;
        int pixelCount = width * height;
        pixels = new byte[pixelCount * channels];

        int[] argb = source.getRGB(0, 0, width, height, null, 0, width);
        for (int i = 0; i < pixelCount; i++) {
            pixels[i * channels] = (byte) ((argb[i] >> 16) & 0xFF);
            pixels[i * channels + 1] = (byte) ((argb[i] >> 8) & 0xFF);
            pixels[i * channels + 2] = (byte) (argb[i] & 0xFF);
            pixels[i * channels + 3] = (byte) ((argb[i] >> 24) & 0xFF);
        }

        return true;
    }

    /**
     * Сохраняет изображение в файл в формате PNG.
     *
     * @param filename путь к файлу изображения.
     * @return {@code true}, если изображение успешно сохранено.
     */
    public boolean save(String filename) {
        if (pixels.length == 0 || width <= 0 || height <= 0) {
            return false;
        }

        BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int[] argb = new int[width * height];
        for (int i = 0; i < argb.length; i++) {
            int red = pixels[i * channels] & 0xFF;
            int green = pixels[i * channels + 1] & 0xFF;
            int blue = pixels[i * channels + 2] & 0xFF;
            int alpha = pixels[i * channels + 3] & 0xFF;
            argb[i] = (alpha << 24) | (red << 16) | (green << 8) | blue;
        }
        image.setRGB(0, 0, width, height, argb, 0, width);

        try {
            return ImageIO.write(image, "png", new File(filename));
        } catch (IOException _) {
            return false;
        }
    }

    /**
     * Переводит изображение в оттенки серого.
     */
    public void grayscale() {
        if (pixels.length == 0) {
            return;
        }

        for (int i = 0; i < pixels.length; i += channels) {
            int gray = (int) (0.299 * (pixels[i] & 0xFF)
                + 0.587 * (pixels[i + 1] & 0xFF)
                + 0.114 * (pixels[i + 2] & 0xFF));
            byte value = (byte) gray;
            pixels[i] = value;
            pixels[i + 1] = value;
            pixels[i + 2] = value;
        }
    }

    /**
     * Инвертирует цвета изображения.
     */
    public void invert() {
        if (pixels.length == 0) {
            return;
        }

        for (int i = 0; i < pixels.length; i += channels) {
            pixels[i] = (byte) (255 - (pixels[i] & 0xFF));
            pixels[i + 1] = (byte) (255 - (pixels[i + 1] & 0xFF));
            pixels[i + 2] = (byte) (255 - (pixels[i + 2] & 0xFF));
        }
    }
}
