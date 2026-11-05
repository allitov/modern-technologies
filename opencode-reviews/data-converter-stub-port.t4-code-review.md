# Code Review — T4 (Lombok `@UtilityClass` + `log.error` in `Main`)

> Saved verbatim by the orchestrator: the reviewer sandbox could not write files.

**Subject:** `data-converter-stub-port` / T4
**Reviewed:** current on-disk state of `data-converter/src/main/java/io/allitov/dataconverter/Converter.java` and `data-converter/src/main/java/io/allitov/dataconverter/Main.java`
**Spec:** human's verbatim review comments (T4 amendment waives the "no Lombok / no logging" contract constraint; build file intentionally untouched; deps per human order).
**Method:** read-only review of the two changed files + their direct context (tests, `ConversionException`, build file, version catalog). Sandbox denied shell and file-write permissions, so no `git add`/`git diff` staging, no report file written, and Gradle not re-run (per instructions); compile/test greenness taken from the writer's reported `:data-converter:compileJava` GREEN and `:data-converter:test` GREEN (13 tests).

## Verdict: **PASS**

No violations found. Both human comments are implemented exactly, with zero behavior drift.

---

## Findings

### Converter.java

- **PASS** `Converter.java:9,11` — `@UtilityClass` present and imported from `lombok.experimental.UtilityClass`, which is the correct package (it does not live in Lombok's root package).
- **PASS** `Converter.java:12` (whole file) — no explicit constructor remains; no `private Converter()` anywhere (grep across `src/main/java`). Lombok generates the private constructor that throws `UnsupportedOperationException`.
- **PASS** — no `static` keyword on any method: public `convertText`/`convertFile` and private helpers `normalizeExt`/`extension`/`readInput`/`writeOutput` are all declared without `static`. `@UtilityClass` auto-statics them, so static callers compile unchanged. The only `static` in the main source tree is `Main.main` (entry point — must stay static, outside the comment's scope).
- **PASS** — public signatures unchanged: `String convertText(String, String, String)`, `void convertFile(String, String)`. Callers unchanged: `Main.java:14` calls `Converter.convertFile(...)`; all 13 tests call `Converter.convertText`/`Converter.convertFile` statically.
- **PASS** (byte-identical message cross-check against `ConverterTest`):
  - `Converter.java:18` `"Input and output formats must differ"` ↔ `ConverterTest.java:47,55`
  - `Converter.java:20` `"Conversion from " + inExt + " to " + outExt + " is not implemented"` ↔ `ConverterTest.java:39,63,71,79,87,121`
  - `Converter.java:40` `"Unsupported file extension: " + ext` ↔ `ConverterTest.java:23,31`
  - `Converter.java:48` `"File has no extension: " + path` ↔ `ConverterTest.java:98,133`
  - `Converter.java:57` `"Cannot open input file: " + path` ↔ `ConverterTest.java:109`
  - `Converter.java:65` `"Cannot open output file: " + path` — same construction; ordering pinned by `convertFileFailsBeforeCreatingOutputFile`.
- **PASS** `Converter.java:24-28` — `convertFile` ordering intact: `extension(inputPath)` → `extension(outputPath)` → `readInput` → `convertText` → `writeOutput`. Fail-before-output-write preserved, pinned by `ConverterTest.java:113-123` (`assertFalse(Files.exists(output))`) and `ConverterTest.java:126-134` (extension check precedes input read).
- **PASS** `Converter.java:31-43` — normalization logic unchanged: `Locale.ROOT` lowercase, leading-dot strip, `yml` → `yaml` alias, csv/json/yaml allow-list.
- Note (not a finding): explicit `final` on the class is redundant — `@UtilityClass` makes the class final anyway — but harmless, legal, and consistent with the original declaration. Not flagged.

### Main.java

- **PASS** `Main.java:3,5` — `@Slf4j` present, imported from `lombok.extern.slf4j.Slf4j`; generates `private static final org.slf4j.Logger log` (field name `log` — matches the rule "Use the Lombok @Slf4j annotation for logging in production code"). SLF4J API resolves through the declared `implementation(libs.log4j.slf4j)` → `log4j-slf4j2-impl` bridge (`data-converter/build.gradle.kts:16`, `gradle/libs.versions.toml:15`); Lombok annotation processing supplied by `alias(libs.plugins.lombok)` (`data-converter/build.gradle.kts:3`). Build file untouched, as ordered.
- **PASS** `Main.java:10` — former stderr usage site replaced: `log.error("Usage: dataconv <input_file> <output_file>")` followed by `System.exit(2)` (`:11`). `log.error` for the usage line is the human's explicit choice — not relitigated.
- **PASS** `Main.java:17-18` — former stderr error site replaced: `log.error("dataconv: {}", message != null ? message : e.toString())`. Null-message fallback preserved verbatim as the logged argument (SLF4J parameterized form of the previous concatenation — identical rendered text). `e.toString()` can never be `null`, so no new NPE surface.
- **PASS** `Main.java:9-20` — control flow and exit codes exactly preserved: `args.length != 2` → usage log → **exit(2)**; success → **exit(0)**; `catch (Exception)` → error log → **exit(1)**. No branch reordering, no additional/removed conditions.
- **PASS** (grep across `data-converter/src/main/java`) — zero `System.err` occurrences remain. `public static void main` untouched (`Main.java:8`).

### Tests / related files

- **PASS** `ConverterTest.java` — untouched by this task: no Lombok/logging imports, purely the pre-existing pinned suite of 13 `@Test` methods; every pinned message matches current `Converter` byte-for-byte. Nothing needed to change. (Byte-level untouchedness could not be proven via `git diff` because the sandbox denied shell access; content is fully consistent with the pre-existing pinned suite and the writer's green 13-test run.)
- **PASS** `ConversionException.java` — plain, unchanged helper; no review surface in this task.

---

## Summary

| Expectation | Result |
|---|---|
| `@UtilityClass` with correct `lombok.experimental` import | ✅ |
| No explicit constructor in `Converter` | ✅ |
| No `static` on any `Converter` method | ✅ |
| Messages/signatures/logic unchanged; static callers unaffected | ✅ |
| `convertFile` fail-before-output-write ordering intact | ✅ |
| `@Slf4j` with `log` field (SLF4J via log4j-slf4j2-impl) | ✅ |
| Both `System.err` sites → `log.error(...)` | ✅ |
| Exit codes 2/0/1 and control flow preserved | ✅ |
| Null-message fallback preserved as logged argument | ✅ |
| Tests untouched (13 tests, green per writer) | ✅ |
| Build file intentionally untouched; deps already present | ✅ |

**Verdict: PASS** — no required fixes.
