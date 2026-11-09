package io.allitov.imgprocessor;

import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * Пакетная обработка изображений.
 *
 * <p>Программа интерактивно запрашивает входной и выходной каталоги, операцию
 * обработки и режим выполнения, после чего обрабатывает все поддерживаемые
 * изображения из входного каталога и сохраняет результаты в формате PNG
 * в выходной каталог.</p>
 *
 * <p>Многопоточность построена на современных средствах {@code java.util.concurrent}:
 * виртуальные потоки ({@link Executors#newThreadPerTaskExecutor(ThreadFactory)}),
 * {@link BlockingQueue} как общая очередь задач (замена связки queue + mutex),
 * атомарные счётчики вместо блокировок. Все операции с многопоточностью
 * находятся вне класса {@link Image}. Весь вывод программы ведется в лог,
 * ввод параметров выполняется через консоль.</p>
 */
@Slf4j
public class Main {

    // Рабочим каталогом при запуске через Gradle является каталог модуля,
    // поэтому пути по умолчанию ищутся относительно него, а затем относительно
    // корня репозитория.
    private static final Path DEFAULT_INPUT_DIR = resolveDefaultDir(
        "src/main/resources/input", "img-processor/src/main/resources/input");

    private static final Path DEFAULT_OUTPUT_DIR = resolveDefaultDir(
        "src/main/resources/output", "img-processor/src/main/resources/output");

    /**
     * Сигнал завершения работы для воркеров: реальный файл не может
     * иметь пустой путь.
     */
    private static final Path POISON_PILL = Path.of("");

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of(".png", ".jpg", ".jpeg", ".bmp", ".gif", ".tif", ".tiff");

    static void main() {
        Scanner scanner = new Scanner(System.in);
        log.info("IMAGE PROCESSING UTILITY");

        Path inputDir = readDirectory(scanner, "Enter input directory:", DEFAULT_INPUT_DIR);
        Path outputDir = readDirectory(scanner, "Enter output directory:", DEFAULT_OUTPUT_DIR);

        log.info("Scanning input directory...");
        List<Path> files = listImageFiles(inputDir);
        if (files.isEmpty()) {
            log.warn("No supported image files found in {}", inputDir);
            return;
        }

        log.info("Found {} supported image files.", files.size());

        Operation operation = readOperation(scanner);
        ProcessingMode mode = readMode(scanner);
        int workerCount = mode == ProcessingMode.PARALLEL ? readWorkerCount(scanner) : 1;

        if (!createOutputDirectory(outputDir)) {
            return;
        }

        log.info("Обработка запущена: вход '{}', выход '{}', {} файлов, операция '{}', режим '{}', воркеров: {}",
            inputDir, outputDir, files.size(), operation.getLabel(), mode.getLabel(), workerCount);

        Stats stats = new Stats();
        long start = System.nanoTime();
        processFiles(files, outputDir, operation, workerCount, stats);
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        log.info("Обработка завершена за {} мс: успешно {}, с ошибками {}",
            (long) (seconds * 1000), stats.processed(), stats.failed());

        logSummary(mode, operation, workerCount, files.size(), stats, seconds, outputDir);
    }

    /**
     * Обрабатывает файлы из очереди в указанном числе воркеров
     * и дожидается завершения обработки.
     *
     * @param files список файлов.
     * @param outputDir выходной каталог.
     * @param operation выполняемая операция.
     * @param workerCount число воркеров; значение {@code 1} означает
     * последовательную обработку.
     * @param stats счётчики результатов обработки.
     */
    private static void processFiles(List<Path> files, Path outputDir, Operation operation, int workerCount, Stats stats) {
        BlockingQueue<Path> queue = new LinkedBlockingQueue<>(files);
        for (int i = 0; i < workerCount; i++) {
            queue.add(POISON_PILL);
        }

        ThreadFactory threadFactory = Thread.ofVirtual().name("Thread ", 1).factory();
        try (ExecutorService executor = Executors.newThreadPerTaskExecutor(threadFactory)) {
            for (int i = 0; i < workerCount; i++) {
                executor.submit(() -> processQueue(queue, outputDir, operation, stats));
            }
        }
    }

    /**
     * Извлекает файлы из общей очереди и обрабатывает их,
     * пока не встретит сигнал завершения работы.
     *
     * @param queue общая очередь файлов.
     * @param outputDir выходной каталог.
     * @param operation выполняемая операция.
     * @param stats счётчики результатов обработки.
     */
    private static void processQueue(BlockingQueue<Path> queue, Path outputDir, Operation operation, Stats stats) {
        try {
            while (true) {
                Path file = queue.take();
                if (file.equals(POISON_PILL)) {
                    break;
                }

                processImage(file, outputDir, operation, stats);
            }
        } catch (InterruptedException _) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * Загружает изображение, применяет выбранную операцию
     * и сохраняет результат в формате PNG.
     *
     * @param file обрабатываемый файл.
     * @param outputDir выходной каталог.
     * @param operation выполняемая операция.
     * @param stats счётчики результатов обработки.
     */
    private static void processImage(Path file, Path outputDir, Operation operation, Stats stats) {
        Image image = new Image();
        if (!image.load(file.toString())) {
            log.warn("Не удалось загрузить файл {}", file);
            logProgress(false, file.getFileName().toString());
            stats.record(false);
            return;
        }

        operation.apply(image);

        Path target = outputDir.resolve(stripExtension(file.getFileName().toString()) + ".png");
        boolean saved = image.save(target.toString());
        if (!saved) {
            log.warn("Не удалось сохранить результат в {}", target);
        }

        logProgress(saved, file.getFileName().toString());
        stats.record(saved);
    }

    /**
     * Логирует результат обработки одного файла.
     *
     * @param success признак успешной обработки.
     * @param fileName имя обработанного файла.
     */
    private static void logProgress(boolean success, String fileName) {
        String threadName = Thread.currentThread().getName();
        if (success) {
            log.info("[{}] Completed: {}", threadName, fileName);
        } else {
            log.warn("[{}] Failed: {}", threadName, fileName);
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
     * Создает выходной каталог, если он не существует.
     *
     * @param dir выходной каталог.
     * @return {@code true}, если каталог существует или успешно создан.
     */
    private static boolean createOutputDirectory(Path dir) {
        try {
            Files.createDirectories(dir);
            return true;
        } catch (IOException e) {
            log.error("Не удалось создать выходной каталог {}", dir, e);
            return false;
        }
    }

    /**
     * Возвращает каталог по умолчанию с учетом рабочего каталога запуска:
     * путь проверяется сначала относительно каталога модуля (так работает
     * {@code gradlew :img-processor:run}), затем относительно корня репозитория.
     *
     * @param relativeToModule путь относительно каталога модуля.
     * @param relativeToRoot путь относительно корня репозитория.
     * @return первый существующий путь; если ни один не существует,
     * возвращается путь относительно каталога модуля.
     */
    private static Path resolveDefaultDir(String relativeToModule, String relativeToRoot) {
        Path moduleRelative = Path.of(relativeToModule);
        return Files.isDirectory(moduleRelative) ? moduleRelative : Path.of(relativeToRoot);
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
     * Запрашивает у пользователя путь к каталогу.
     * Пустой ввод означает каталог по умолчанию.
     *
     * @param scanner источник консольного ввода.
     * @param prompt текст приглашения.
     * @param defaultDir каталог по умолчанию.
     * @return путь к каталогу.
     */
    private static Path readDirectory(Scanner scanner, String prompt, Path defaultDir) {
        while (true) {
            log.info(prompt);
            String line = nextLine(scanner).trim();
            if (line.isEmpty()) {
                return defaultDir;
            }

            try {
                return Path.of(line);
            } catch (InvalidPathException _) {
                log.warn("Invalid path, please try again.");
            }
        }
    }

    /**
     * Запрашивает у пользователя операцию обработки.
     *
     * @param scanner источник консольного ввода.
     * @return выбранная операция.
     */
    private static Operation readOperation(Scanner scanner) {
        log.info("Select processing operation:");
        for (Operation operation : Operation.values()) {
            log.info("  {}. {}", operation.getNumber(), operation.getLabel());
        }

        while (true) {
            log.info("Enter operation number:");
            Integer number = tryParseInt(nextLine(scanner).trim());
            Optional<Operation> operation = number == null ? Optional.empty() : Operation.fromNumber(number);
            if (operation.isPresent()) {
                return operation.get();
            }

            log.warn("Invalid choice, please enter a number from 1 to {}.", Operation.values().length);
        }
    }

    /**
     * Запрашивает у пользователя режим обработки.
     *
     * @param scanner источник консольного ввода.
     * @return выбранный режим.
     */
    private static ProcessingMode readMode(Scanner scanner) {
        log.info("Select processing mode:");
        for (ProcessingMode mode : ProcessingMode.values()) {
            log.info("  {}. {} processing", mode.getNumber(), mode.getLabel());
        }

        while (true) {
            log.info("Enter mode:");
            Integer number = tryParseInt(nextLine(scanner).trim());
            Optional<ProcessingMode> mode = number == null ? Optional.empty() : ProcessingMode.fromNumber(number);
            if (mode.isPresent()) {
                return mode.get();
            }

            log.warn("Invalid choice, please enter a number from 1 to {}.", ProcessingMode.values().length);
        }
    }

    /**
     * Запрашивает у пользователя число потоков-воркеров.
     * Пустой ввод означает число доступных процессорных ядер.
     *
     * @param scanner источник консольного ввода.
     * @return положительное число воркеров.
     */
    private static int readWorkerCount(Scanner scanner) {
        int defaultWorkers = Runtime.getRuntime().availableProcessors();
        while (true) {
            log.info("Enter number of worker threads (> 0):");
            String line = nextLine(scanner).trim();
            if (line.isEmpty()) {
                log.info("Using default: {}", defaultWorkers);
                return defaultWorkers;
            }

            Integer count = tryParseInt(line);
            if (count != null && count > 0) {
                return count;
            }

            log.warn("Please enter a positive integer.");
        }
    }

    /**
     * Читает строку из потока ввода, завершая работу при его закрытии.
     *
     * @param scanner источник консольного ввода.
     * @return прочитанная строка.
     */
    private static String nextLine(Scanner scanner) {
        if (!scanner.hasNextLine()) {
            log.error("Поток ввода закрыт, работа завершена.");
            System.exit(1);
        }

        return scanner.nextLine();
    }

    /**
     * Разбирает строку в целое число.
     *
     * @param line разбираемая строка.
     * @return число или {@code null}, если строка не является целым числом.
     */
    private static Integer tryParseInt(String line) {
        try {
            return Integer.valueOf(line);
        } catch (NumberFormatException _) {
            return null;
        }
    }

    /**
     * Логирует сводку результатов пакетной обработки.
     *
     * @param mode режим обработки.
     * @param operation выполняемая операция.
     * @param workerCount число воркеров.
     * @param total общее число файлов.
     * @param stats счётчики результатов обработки.
     * @param seconds время обработки в секундах.
     * @param outputDir выходной каталог.
     */
    private static void logSummary(ProcessingMode mode, Operation operation, int workerCount, int total,
                                   Stats stats, double seconds, Path outputDir) {
        log.info("Processing Summary");
        log.info("Mode: {}", mode.getLabel());
        log.info("Operation: {}", operation.getLabel());
        log.info("Worker threads: {}", workerCount);
        log.info("Total images: {}", total);
        log.info("Successfully processed: {}", stats.processed());
        log.info("Failed: {}", stats.failed());
        log.info("Processing time: {} seconds", String.format(Locale.ROOT, "%.3f", seconds));
        log.info("Results directory: {}", outputDir);
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

        void record(boolean success) {
            if (success) {
                processed.incrementAndGet();
            } else {
                failed.incrementAndGet();
            }
        }
    }
}
