# Plan contract: data-converter-stub-port

> Disk source of truth. Re-read before each decision and after any compaction.

**Goal:** Заменить неотслеживаемые WIP-исходники модуля `data-converter` новыми, написанными с нуля на чистом JDK
(ноль внешних зависимостей): публичный API зеркалит C++-оригинал `DataConverter/` (`convertText` / `convertFile` /
`ConversionException` / CLI `Main` с теми же кодами выхода), нормализация расширений и контракт ошибок — реальные,
сама конвертация во все форматы — заглушки, бросающие `ConversionException` «not implemented». Модуль собирается
(`.\gradlew.bat :data-converter:compileJava`) и покрыт юнит-тестами на JUnit Jupiter (тоже без новых зависимостей),
зеркалящими применимое подмножество 13 GTest-кейсов C++ (`DataConverter/tests/converter_tests.cpp`).

**active:** none — T1–T4 complete (all gates PASS)

## open_questions

- [x] Q1: Как трактовать «заглушки, чтобы не подключать доп. библиотеки»? — answer: **Всё — заглушки**: все
  конвертации бросают «not implemented»; тесты — на нормализацию расширений, отказ same-format и поведение заглушек.
- [x] Q2: Что делать с существующими WIP-исходниками (Converter.java 559 строк на Jackson/SnakeYAML/Lombok,
  ConversionException.java, Main.java — модуль не компилируется, зависимости не объявлены)? — answer:
  **Написать с нуля**: удалить `data-converter/src/` целиком и написать файлы заново по этому контракту.

## backlog

- [x] **T1** — Модуль data-converter с нуля: чистый JDK, конвертации — заглушки
    - **agent:** coder
    - **inputs:**
        - Удалить целиком `data-converter/src/` (неотслеживаемый WIP на Jackson/SnakeYAML/Lombok — решение человека:
          писать с нуля), затем создать заново ТОЛЬКО следующие файлы (пакет `io.allitov.dataconverter`):
        - `data-converter/src/main/java/io/allitov/dataconverter/ConversionException.java` —
          `public class ConversionException extends RuntimeException`, единственный конструктор
          `ConversionException(String message)` → `super(message)`. Только JDK.
        - `data-converter/src/main/java/io/allitov/dataconverter/Converter.java` — `public final class Converter` с
          явным `private Converter() {}` (НЕ Lombok, никакой кодогенерации), все методы `static`. Публичный API
          (стабильный, зеркалит `DataConverter/src/converter.hpp:7-11`):
            - `public static String convertText(String input, String inputExtension, String outputExtension)`
            - `public static void convertFile(String inputPath, String outputPath)`
          Поведение (паритет с `DataConverter/src/converter.cpp`, кроме самой конвертации):
            1. Приватный `normalizeExt(String)`: ASCII-lowercase → отрезать ОДНУ ведущую точку, если есть → алиас
               `yml`→`yaml` → допуск только `csv`/`json`/`yaml`, иначе
               `throw new ConversionException("Unsupported file extension: " + ext)` (в сообщении уже
               нормализованное имя; пустая строка допустима в сообщении — паритет C++ `converter.cpp:25-33,30`).
            2. `convertText`: нормализовать оба расширения; если равны →
               `ConversionException("Input and output formats must differ")` (проверка регистронезависимая, т.к.
               сравнение после lowercase — паритет `converter.cpp:222`). Далее — ЗАГЛУШКА:
               `throw new ConversionException("Conversion from " + inExt + " to " + outExt + " is not implemented")`
               (в сообщении нормализованные имена; parse/emit-логики НЕТ вовсе).
            3. `convertFile` — тонкая реальная оболочка (чистый JDK, паритет `converter.cpp:229-246`):
               (a) расширение из ВСЕЙ строки пути: `lastIndexOf('.')`; нет точки →
               `ConversionException("File has no extension: " + path)`; (b) `Files.readString(Path, UTF_8)`,
               `IOException` → `ConversionException("Cannot open input file: " + path)`; (c) вызвать `convertText`
               (он бросит not-implemented ДО открытия выходного файла — выходной файл не создаётся/не меняется);
               (d) записать результат `Files.newBufferedWriter(..., UTF_8)`, `IOException` →
               `ConversionException("Cannot open output file: " + path)`. Переводы строк — только `\n` (никаких
               платформенных `line.separator`).
        - `data-converter/src/main/java/io/allitov/dataconverter/Main.java` — `public final class Main`, явный
          `private Main() {}`, `public static void main(String[] args)` (паритет `DataConverter/src/main.cpp:6-18`):
          `args.length != 2` → в stderr `"Usage: dataconv <input_file> <output_file>\n"` и `System.exit(2)`; иначе
          `Converter.convertFile(args[0], args[1])` и `System.exit(0)`; любой `Exception` → в stderr
          `"dataconv: " + message` (message==null → `e.toString()`) и `System.exit(1)`. Логгирования нет.
        - `data-converter/build.gradle.kts` — оставить как есть, ДОБАВИТЬ только блок toolchain 25 (скопировать по
          форме из `todo-app/build.gradle.kts:9-13`; `pvz/build.gradle.kts:11-15` — второй пример). НЕ добавлять
          никаких зависимостей (jackson/snakeyaml/lombok запрещены решением человека; junit-bom 6.0.0 + jupiter +
          launcher уже объявлены на строках 12-16, `useJUnitPlatform` на 18-20 — не трогать).
        - Эталон для сверки сообщений: `DataConverter/src/converter.cpp` (все throw-сайты перечислены в этом
          контракте; C++ можно читать, копировать его логику конвертации НЕ нужно — она заглушена).
    - **deps:** []
    - **acceptance:** `.\gradlew.bat :data-converter:compileJava` зелёный; в `data-converter/` grep по
      `jackson|snakeyaml|lombok|fasterxml|org.yaml` не находит НИЧЕГО (ни исходников, ни build-файла); в
      `data-converter/build.gradle.kts` присутствует toolchain 25; публичный API `Converter` — ровно два метода
      `convertText`/`convertFile` с сигнатурами выше; `Main` компилируется и содержит exit-коды 2/0/1.
    - **description:** Снести неотслеживаемый WIP `data-converter/src/` и написать с нуля `ConversionException`,
      `Converter` (нормализация расширений и same-format проверка — реальные, конвертация — заглушка
      «not implemented»), `Main` (CLI-оболочка с кодами выхода 2/0/1) — всё на чистом JDK без единой внешней
      зависимости; добавить в `build.gradle.kts` toolchain 25.
    - **amendment (human, live — supersede part of the spec/acceptance):**
        - `data-converter/build.gradle.kts` — NOT to be modified: existing dependencies STAY
          (`alias(libs.plugins.lombok)`, `implementation(libs.log4j.slf4j)`, `testImplementation(libs.assertj)`,
          junit trio, toolchain 25 already present). The acceptance criterion «grep
          `jackson|snakeyaml|lombok|fasterxml|org.yaml` finds NOTHING» is waived for the build file by human
          decision; the new Java sources still contain no forbidden imports.
        - `data-converter/src/` — work with it AS-IS: nothing under `src/` is deleted; the stale WIP resource
          `data-converter/src/main/resources/log4j2.xml` stays. Status: new pure-JDK
          `ConversionException`/`Converter`/`Main` written, `.\gradlew.bat :data-converter:compileJava` GREEN.
        - **gate:** code-review **PASS** → `opencode-reviews/data-converter-stub-port.code-review.md`
          (reviewer sandbox could not write files; report saved by orchestrator verbatim). Regression run: no
          existing tests cover this brand-new module — coverage arrives with T2.
    - **touched:**
        - `data-converter/src/main/java/io/allitov/dataconverter/ConversionException.java` (new)
        - `data-converter/src/main/java/io/allitov/dataconverter/Converter.java` (new)
        - `data-converter/src/main/java/io/allitov/dataconverter/Main.java` (rewritten; old WIP version replaced)
- [x] **T2** — Юнит-тесты ConverterTest: паритет применимого подмножества 13 GTest-кейсов
    - **agent:** tester
    - **inputs:**
        - Тестируемый класс: `data-converter/src/main/java/io/allitov/dataconverter/Converter.java` (создан в T1;
          читать только его, `ConversionException.java` и `Main.java` — узко, не весь проект).
        - ОРИГИНАЛ для паритета: `DataConverter/tests/converter_tests.cpp:11-74` (13 кейсов). В режиме заглушек
          применимо НЕ всё: кейсы с реальной конвертацией (CsvToJson, CsvQuotedComma, CsvEscapedQuote, JsonToCsv,
          JsonToYaml, YamlToJson, MalformedJsonRejected, EmptyCsvProducesEmptyArray, CsvWrongColumnCount,
          NestedObjectCannotExportToCsv) НЕ портируются — вместо них тесты заглушек (см. матрицу).
        - Обязательная матрица тестов (файл
          `data-converter/src/test/java/io/allitov/dataconverter/ConverterTest.java`):
            1. unsupported extension: `convertText("x", "xml", "json")` → ConversionException с сообщением
               `Unsupported file extension: xml` (паритет GTest InvalidExtension);
            2. расширение с ведущей точкой и в верхнем регистре: `convertText("x", ".XML", "json")` → то же
               сообщение `Unsupported file extension: xml` (lowercase + strip dot);
            3. алиас yml принимается нормализацией: `convertText("x", "yml", "json")` → сообщение
               `Conversion from yaml to json is not implemented` (именно оно — доказывает, что алиас сработал и
               ошибка НЕ «Unsupported file extension»); паритет GTest YmlExtensionAccepted;
            4. same-format отклоняется: `convertText("[]", "json", "json")` →
               `Input and output formats must differ` (паритет GTest SameFormatRejected);
            5. same-format регистронезависим: `convertText("[]", "JSON", "json")` → то же сообщение;
            6. заглушка csv→json: `convertText("a,b\n", "csv", "json")` →
               `Conversion from csv to json is not implemented`;
            7. заглушка json→csv: `convertText("[]", "json", "csv")` →
               `Conversion from json to csv is not implemented`;
            8. заглушка yaml→json: `convertText("- a: 1\n", "yaml", "json")` →
               `Conversion from yaml to json is not implemented`;
            9. заглушка yaml→csv: `convertText("- a: 1\n", "yaml", "csv")` →
               `Conversion from yaml to csv is not implemented`;
            10. convertFile, нет расширения: файл без точки во временном каталоге →
                `File has no extension: <путь>` (паритет converter.cpp:232);
            11. convertFile, вход не существует: `convertFile(tmp + "/missing.csv", tmp + "/out.json")` →
                `Cannot open input file: <путь>`;
            12. convertFile, валидный вход: создать `in.csv` с `a,b\n1,2\n`, вызвать `convertFile(in, out)` →
                ConversionException «not implemented» И выходной файл НЕ создан
                (`Files.exists(out)` == false — конвертация падает до записи);
            13. (опционально, если чисто делается) порядок проверки: `convertFile` для входа без расширения падает
                ДО чтения файла (файл может не существовать — ошибка всё равно «File has no extension»).
        - Стиль и инфраструктура (конвенции соседей — `todo-app/.../TaskTest.java:14-21`): package-private тестовый
          класс, plain `@Test`, описательные camelCase имена методов, БЕЗ `@DisplayName`; JUnit Jupiter 6
          (junit-bom уже в build). Ассерты — ТОЛЬКО `org.junit.jupiter.api.Assertions` (assertEquals/
          assertThrows/...): AssertJ НЕ добавлять — ноль новых зависимостей. Временные файлы —
          `@TempDir` из `org.junit.jupiter.api.io.TempDir` (в репо первое использование — просто завести). Для
          ассерта сообщений: `ConversionException ex = assertThrows(...); assertEquals("...", ex.getMessage())`.
          Ничего про `Main`/System.exit не тестировать (вне скоупа).
        - Команда прогона: `.\gradlew.bat :data-converter:test` (Windows; Gradle wrapper 9.6.0).
    - **deps:** [T1]
    - **acceptance:** `.\gradlew.bat :data-converter:test` зелёный; в `ConverterTest.java` присутствуют все пункты
      матрицы 1–12 (минимум 12 тестовых методов); grep по `data-converter/build.gradle.kts` не показывает новых
      зависимостей сверх имевшихся junit-трёх; каждый тест ассертит точное сообщение исключения (не просто факт
      броска), кроме пунктов 12/13 где дополнительно ассертится отсутствие выходного файла.
    - **description:** Написать `ConverterTest.java` на чистом JUnit Jupiter (без новых зависимостей): матрица из
      12 кейсов — нормализация расширений, алиас yml, same-format (включая регистр), «not implemented» для всех
      пар форматов, ошибки convertFile и гарантия «выходной файл не создан» через @TempDir.
    - **touched:**
        - `data-converter/src/test/java/io/allitov/dataconverter/ConverterTest.java` (new; 13 тестов = матрица
          1–13, `.\gradlew.bat :data-converter:test` зелёный: tests=13, failures=0, errors=0; build.gradle.kts
          и src/main не тронуты)
- [x] **T3** — Ревью тестов (gate)
    - **agent:** test-reviewer
    - **inputs:** ref — файлы `data-converter/src/test/java/io/allitov/dataconverter/ConverterTest.java` и
      тестируемые `Converter.java`/`ConversionException.java` (git-статус: untracked, ревью по рабочему дереву);
      rubric-контекст: тесты должны ассертить точные сообщения, покрывать матрицу из T2, не добавлять зависимостей.
    - **deps:** [T2]
    - **acceptance:** отчёт записан в `opencode-reviews/data-converter-stub-port.test-review.md`, вердикт
      PASS/FAIL возвращён в ответе; при FAIL — перечень конкретных правок.
    - **description:** Standalone-ревью качества новых юнит-тестов конвертера (gate после writer по правилу
      explorer→writer→review); отчёт в opencode-reviews/.
    - **touched:**
        - `opencode-reviews/data-converter-stub-port.test-review.md` (new; вердикт **PASS**, отчёт сохранён
          оркестратором дословно — песочница ревьюера не пишет файлы)
- [x] **T4** — Правки по комментариям человека: Lombok `@UtilityClass` + `log.error` в `Main`
    - **agent:** coder
    - **inputs:**
        - **Amendment (human, live — supersede the contract's «никакого lombok / логгирования нет»):** now use
          the already-declared deps (lombok plugin + `libs.log4j.slf4j`); the original pure-JDK/no-logging
          constraint no longer applies to these two files.
        - Comment 1 (`Converter.java`, quoted at class decl + private ctor): make it a utility via Lombok
          `@UtilityClass`; then delete `static` from ALL methods (public `convertText`/`convertFile` AND
          private helpers `normalizeExt`/`extension`/`readInput`/`writeOutput`) and DELETE the explicit
          private constructor (Lombok generates its own). Behavior and public call shape unchanged —
          `@UtilityClass` auto-statics members, so `Main` and `ConverterTest` keep compiling as-is.
        - Comment 2 (`Main.java`, BOTH quoted sites): replace stderr printing with `log.error(...)`:
          (a) usage line 10 `"Usage: dataconv <input_file> <output_file>\n"` → `log.error(...)`;
          (b) line 18 `"dataconv: " + (message != null ? message : e.toString())` → `log.error(...)`.
          Control flow and exit codes 2/0/1 must NOT change. Logger setup follows repo convention: check
          `gradle/libs.versions.toml` for the exact `libs.log4j.slf4j` artifact and how neighbors (todo-app)
          declare loggers — expected either Lombok `@Slf4j` (field `log`, SLF4J API) or
          `org.apache.logging.log4j` `LogManager`; pick what actually resolves on this module's classpath.
          Do NOT add dependencies, do NOT touch `build.gradle.kts`.
        - `data-converter/src/main/resources/log4j2.xml` — keep as-is (now actually relevant for log4j config).
    - **deps:** []
    - **acceptance:** `.\gradlew.bat :data-converter:compileJava` green; `Converter.java` has `@UtilityClass`,
      no `static` keyword on methods, no explicit constructor; `Main.java` has no `System.err`, `log.error`
      at both former stderr sites, exit codes 2/0/1 intact; regression `.\gradlew.bat :data-converter:test`
      green (ConverterTest, 13 tests).
    - **description:** Apply human review comments: `Converter` → Lombok `@UtilityClass` (drop explicit
      `static` modifiers and the explicit constructor), `Main` → `log.error()` for usage and error output —
      no behavior changes, no build-file changes.
    - **touched:**
        - `data-converter/src/main/java/io/allitov/dataconverter/Converter.java` (modified: `@UtilityClass`
          из `lombok.experimental`, явный конструктор удалён, `static` снят со всех 6 методов)
        - `data-converter/src/main/java/io/allitov/dataconverter/Main.java` (modified: `@Slf4j`, оба
          `System.err`-сайта → `log.error`, exit-коды 2/0/1 сохранены)
        - gates: code-review **PASS** → `opencode-reviews/data-converter-stub-port.t4-code-review.md`;
          regression `:data-converter:test --rerun` **GREEN** (tests=13, failures=0, errors=0; тесты не
          менялись). Оба отчёта сохранены оркестратором — песочницы субагентов не пишут файлы.

## Примечания для исполнителей (не задачи)

- Ноль внешних зависимостей — жёсткое ограничение от человека: НИ jackson, НИ snakeyaml, НИ lombok (в т.ч. через
  freefair-плагин как в todo-app), НИ AssertJ в тестах. Только JDK + уже объявленные junit-bom/jupiter/launcher.
- `trim` в C++ (`converter.cpp:35-40`) — мёртвый код (нет call-sites), в порте НЕ воспроизводить.
- Сообщение заглушки — единственное НОВОЕ сообщение (в C++ его нет): точный формат
  `Conversion from <inExt> to <outExt> is not implemented`, `<inExt>`/`<outExt>` — уже нормализованные имена.
- Строки в сообщениях об ошибках — дословно как в контракте (тесты T2 ассертят их буквально).
