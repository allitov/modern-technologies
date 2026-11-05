# Code review: T1 — data-converter stub port (pure JDK)

> Saved verbatim by the orchestrator: the reviewer sandbox could not write files.

- **Task:** T1 of `opencode-plans/data-converter-stub-port.md` (incl. «amendment (human, live)»)
- **Scope (working tree; files git-untracked/new — reviewed as-is):** `ConversionException.java` (new), `Converter.java` (new), `Main.java` (rewritten), package `io.allitov.dataconverter`
- **References:** `DataConverter/src/converter.cpp` (lines 19-46, 217-248), `DataConverter/src/main.cpp:6-18`, plan contract.
- **Method note:** `git add`/`git diff --staged` unavailable (shell denied in this sandbox); per the task statement the review gate is by working tree, so the on-disk files are the diff. `java-rules.md` does not exist in this repo (glob found nothing) — reviewed against the T1 contract. Gradle compile **not** re-run (per instructions); writer reported BUILD SUCCESSFUL, and sources are syntactically consistent on read.

## Verdict: **PASS**

### 1. Package & purity
- PASS — all three files declare `package io.allitov.dataconverter;`.
- PASS — grep `jackson|snakeyaml|lombok|fasterxml|org.yaml` (case-insensitive) over `data-converter/src/main/java`: **no matches**. Only imports: `java.io.*`, `java.nio.charset.StandardCharsets`, `java.nio.file.*`, `java.util.Locale` — pure JDK.
- PASS — source tree holds exactly the three new Java files (+ `src/main/resources/log4j2.xml`, waived by the human amendment); no stale WIP Java remains.
- WAIVED (human decision, out of scope) — `data-converter/build.gradle.kts` deps untouched; not reviewed, not flagged.

### 2. ConversionException.java
- PASS `ConversionException.java:3` — `public class ConversionException extends RuntimeException`.
- PASS `ConversionException.java:5-7` — single ctor `(String message)` → `super(message)`; no other members.

### 3. Converter.java — API shape
- PASS `Converter.java:10` — `public final class Converter`; PASS `:12-13` — explicit `private Converter() {}` (no Lombok/codegen).
- PASS — exactly two public static methods: `:15` `convertText(String,String,String)`, `:24` `convertFile(String,String)`; `normalizeExt`/`extension`/`readInput`/`writeOutput` are all `private static`.

### 4. normalizeExt (`Converter.java:32-44`) vs `converter.cpp:25-33`
- PASS `:33` — ASCII-lowercase via `toLowerCase(Locale.ROOT)` (avoids locale-sensitive lowercasing; parity with C++ `std::tolower`).
- PASS `:34-36` — strips exactly ONE leading dot (`"..csv"` → `".csv"` unsupported, same as C++ `:27`).
- PASS `:37-39` — `yml`→`yaml` alias applied before the whitelist, matching C++ order (so `yml` never appears in an error message).
- PASS `:40-41` — only `csv`/`json`/`yaml` allowed; else `ConversionException("Unsupported file extension: " + ext)` with the normalized name (matches `converter.cpp:29-30`); empty string falls through with empty suffix — contract-allowed C++ parity.

### 5. convertText (`Converter.java:15-22`) vs `converter.cpp:217-227`
- PASS `:16-17` both normalized; PASS `:18-20` equal-after-lowercase → `"Input and output formats must differ"` (case-insensitive, parity `:222`).
- PASS `:21` — stub throws exactly `"Conversion from <inExt> to <outExt> is not implemented"` with normalized names; no parse/emit logic anywhere in the file (confirmed by full read).

### 6. convertFile (`Converter.java:24-30, 46-68`) vs `converter.cpp:229-246`
- PASS `:46-52` — extension from the WHOLE path via `lastIndexOf('.')`; no dot → `"File has no extension: " + path`; same edge behavior as C++ `find_last_of`.
- PASS `:25-27` — both extension extractions BEFORE `readInput` (check-before-read). Raw C++ ordering differs (input opened at `:236-239`, extensions lazily at `:241`), but the contract explicitly mandates this ordering (T1 (a)→(b); T2 test #13 codifies it) — deliberate, spec-conformant.
- PASS `:54-60` — `Files.readString(Path.of(path), UTF_8)`; `IOException` → `"Cannot open input file: " + path`.
- PASS `:28-29` — `convertText` runs before `writeOutput`; the stub always throws, so the output file is never created/modified on failure.
- PASS `:62-68` — `Files.newBufferedWriter(..., UTF_8)` in try-with-resources; `IOException` → `"Cannot open output file: " + path`.
- PASS — line endings: no `line.separator` anywhere; only `\n` literal is `Main`'s usage string.
- PASS — C++ `trim` (`converter.cpp:35-40`, dead code) NOT ported: no `trim` symbol in any file.

### 7. Main.java vs `main.cpp:6-18`
- PASS `:3-6` — `public final class Main`, explicit `private Main() {}`.
- PASS `:9-12` — `args.length != 2` → stderr `Usage: dataconv <input_file> <output_file>\n` (literal `\n`, byte-parity with `main.cpp:8`) + exit 2; `System.exit(2)` prevents fallthrough.
- PASS `:14-15` — success → `convertFile(args[0], args[1])` + exit 0.
- PASS `:16-20` — catch `Exception` → stderr `"dataconv: " + (message != null ? message : e.toString())` + exit 1 (null-message fallback per contract; equivalent of C++ `e.what()`).
- PASS — no logging.

### 8. Logic / boundary scan
- `.XML` → `"Unsupported file extension: xml"` (normalized name); `yml` → accepted as `yaml`; `JSON` vs `json` → "must differ" (lowercase-before-compare); stub messages use normalized names.
- JDK signatures verified: `Files.readString(Path, Charset)`, `Files.newBufferedWriter(Path, Charset)`, `toLowerCase(Locale)`, `lastIndexOf(int)` — all correct; no inverted conditions (`dot < 0`, whitelist negation both correct).

### Informational notes (no action required)
1. **[minor, out of contract] `Converter.java:56`/`:63`** — `Path.of(path)` may throw unchecked `InvalidPathException` for invalid path syntax instead of an `IOException`-wrapped `"Cannot open input/output file"`. Contract only mandates wrapping `IOException`, so conformant; flagged for human awareness only.
2. **C++ message not ported** — `converter.cpp:245` `"Failed to write output file: …"` has no Java counterpart; the contract's throw-site list intentionally omits it and item (d) maps all writer `IOException`s to `"Cannot open output file: " + path`. Conformant by design.
3. **Ordering divergence (intentional)** — extension checks precede input reading (contract-mandated) whereas C++ opens input first (`converter.cpp:236` vs `:241`); matches T2 test matrix item #13.

### Summary
`PASS` — T1 matches the contract exactly: pure-JDK sources, correct exception, correct `Converter` API, normalization/error parity with the C++ original, stub-only conversion with exact message formats, output-file-never-created-on-failure guarantee, `Main` exit codes 2/0/1 with exact stderr strings, `trim` correctly not ported. Build-file items waived by human decision and not reviewed.
