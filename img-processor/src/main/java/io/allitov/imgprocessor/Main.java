package io.allitov.imgprocessor;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Пакетная обработка изображений.
 *
 * <p>Программа сканирует входной каталог и параллельно обрабатывает найденные
 * изображения: применяет преобразование в оттенки серого и сохраняет результат
 * в формате PNG в выходной каталог.</p>
 *
 * <p>Многопоточность построена на современных средствах {@code java.util.concurrent}:
 * виртуальные потоки ({@link Executors#newVirtualThreadPerTaskExecutor()}),
 * {@link BlockingQueue} как общая очередь задач (замена связки queue + mutex),
 * атомарные счётчики вместо блокировок. Все операции с многопоточностью
 * находятся вне класса {@link Image}.</p>
 *
 * <p>Использование: {@code Main [входной_каталог] [выходной_каталог]}.
 * По умолчанию обрабатывается каталог {@code src/main/resources/input},
 * результат сохраняется в {@code src/main/resources/output}.</p>
 */
@Slf4j
public class Main {

    private static final Path POISON_PILL = Path.of("");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".bmp", ".gif", ".tif", ".tiff");

    static void main(String[] args) {
        Path inputDir = args.length > 0 ? Path.of(args[0]) : Path.of("src/main/resources/input");
        Path outputDir = args.length > 1 ? Path.of(args[1]) : Path.of("src/main/resources/output");

        List<Path> files = listImageFiles(inputDir);
        if (files.isEmpty()) {
            log.warn("В каталоге {} нет изображений для обработки", inputDir);
            return;
        }

        try {
            Files.createDirectories(outputDir);
        } catch (IOException e) {
            log.error("Не удалось создать выходной каталог {}", outputDir, e);
            return;
        }

        // Обработка пикселей ограничена процессором,
        // поэтому число воркеров равно числу доступных ядер.
        int workerCount = Runtime.getRuntime().availableProcessors();
        BlockingQueue<Path> queue = new LinkedBlockingQueue<>(files);
        // Сигналы завершения стоят в конце очереди, поэтому каждый воркер
        // получит свой только после того, как все файлы будут обработаны.
        for (int i = 0; i < workerCount; i++) {
            queue.add(POISON_PILL);
        }
        Stats stats = new Stats();

        log.info("Обработка запущена: {} файлов, воркеров: {}", files.size(), workerCount);
        long start = System.nanoTime();

        try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 0; i < workerCount; i++) {
                executor.submit(() -> processQueue(queue, outputDir, stats));
            }
        }
        // close() исполнителя дожидается завершения всех воркеров.

        long elapsedMs = (System.nanoTime() - start) / 1_000_000;
        log.info("Обработка завершена за {} мс: успешно {}, с ошибками {}", elapsedMs, stats.processed(), stats.failed());
    }

    /**
     * Извлекает файлы из общей очереди и обрабатывает их,
     * пока не встретит сигнал завершения работы.
     *
     * @param queue общая очередь файлов.
     * @param outputDir выходной каталог.
     * @param stats счётчики результатов обработки.
     */
    private static void processQueue(BlockingQueue<Path> queue, Path outputDir, Stats stats) {
        try {
            while (true) {
                Path file = queue.take();
                if (file.equals(POISON_PILL)) {
                    break;
                }

                processImage(file, outputDir, stats);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Загружает изображение, применяет преобразование в оттенки серого
     * и сохраняет результат в формате PNG.
     *
     * @param file обрабатываемый файл.
     * @param outputDir выходной каталог.
     * @param stats счётчики результатов обработки.
     */
    private static void processImage(Path file, Path outputDir, Stats stats) {
        log.debug("Обработка файла {}", file);

        Image image = new Image();
        if (!image.load(file.toString())) {
            log.warn("Не удалось загрузить файл {}", file);
            stats.incrementFailed();
            return;
        }

        image.grayscale();

        Path target = outputDir.resolve(stripExtension(file.getFileName().toString()) + ".png");
        if (image.save(target.toString())) {
            stats.incrementProcessed();
        } else {
            log.warn("Не удалось сохранить файл {}", target);
            stats.incrementFailed();
        }
    }

    /**
     * Возвращает список поддерживаемых изображений из каталога.
     *
     * @param dir входной каталог.
     * @return отсортированный список путей к файлам; пустой, если каталог
     * не существует, не читается или не содержит поддерживаемых изображений.
     */
    private static List<Path> listImageFiles(Path dir) {
        if (!Files.isDirectory(dir)) {
            return List.of();
        }

        try (Stream<Path> entries = Files.list(dir)) {
            return entries
                .filter(Files::isRegularFile)
                .filter(Main::isSupportedImage)
                .sorted()
                .toList();
        } catch (IOException e) {
            log.error("Не удалось прочитать каталог {}", dir, e);
            return List.of();
        }
    }

    /**
     * Проверяет, что файл имеет поддерживаемое расширение изображения.
     *
     * @param file проверяемый файл.
     * @return {@code true}, если расширение файла поддерживается.
     */
    private static boolean isSupportedImage(Path file) {
        String name = file.getFileName().toString().toLowerCase(Locale.ROOT);
        return SUPPORTED_EXTENSIONS.stream().anyMatch(name::endsWith);
    }

    /**
     * Удаляет расширение из имени файла.
     *
     * @param name имя файла.
     * @return имя файла без расширения.
     */
    private static String stripExtension(String name) {
        int dotIndex = name.lastIndexOf('.');
        return dotIndex > 0 ? name.substring(0, dotIndex) : name;
    }

    /**
     * Счётчики результатов пакетной обработки.
     * Атомарные поля обновляются из разных потоков без блокировок.
     */
    private static final class Stats {

        private final AtomicInteger processed = new AtomicInteger();

        private final AtomicInteger failed = new AtomicInteger();

        int processed() {
            return processed.get();
        }

        int failed() {
            return failed.get();
        }

        void incrementProcessed() {
            processed.incrementAndGet();
        }

        void incrementFailed() {
            failed.incrementAndGet();
        }
    }
}
