# Test review: data-converter-stub-port (T3 gate)

> Saved verbatim by the orchestrator: the reviewer sandbox could not write files.

**Verdict: PASS**

- Subject: `data-converter/src/test/java/io/allitov/dataconverter/ConverterTest.java` (13 tests) against
  `Converter.java` / `ConversionException.java` (T1 stub sources).
- Reviewer: test-reviewer (GLM-5.3-Flash), date: 2026-11-05.
- Plan: `opencode-plans/data-converter-stub-port.md`, task **T3** (gate after T2/tester).
- Review basis: **working tree** (files are git-untracked per plan; the `shell` tool was permission-denied in
  this sandbox, so `git add`/`git diff --staged` could not be run — review performed on full file reads, which
  the plan explicitly prescribes for untracked files). Gradle was NOT re-run per instructions; run status
  (tests=13, failures=0, errors=0) is writer-reported and could-not-verify directly (no `build/test-results`
  XML present at review time), but the review is quality-based and does not depend on it.

## 1. Matrix coverage (contract items 1–13) — all present, in order

Each test asserts the **exact** exception message, per the contract ("каждый тест ассертит точное сообщение
исключения"); items 12/13 additionally assert the mandated extras.

| # | Contract item | Test (`ConverterTest.java`) | Exact-message assert | Parity |
|---|---|---|---|---|
| 1 | unsupported ext `("x","xml","json")` | `convertTextThrowsForUnsupportedExtension` :18–24 | `"Unsupported file extension: xml"` :23 | GTest `InvalidExtension` (converter_tests.cpp:54) |
| 2 | `".XML"` → lowercase + strip dot | `convertTextLowercasesAndStripsLeadingDotInUnsupportedExtensionMessage` :26–32 | `"Unsupported file extension: xml"` :31 | contract item 2 |
| 3 | `yml` alias proof | `convertTextNormalizesYmlAliasToYaml` :34–40 | `"Conversion from yaml to json is not implemented"` :39 — the not-implemented message with **normalized `yaml`**, NOT `"Unsupported file extension: yml"` → alias proven | GTest `YmlExtensionAccepted` (cpp:45), stub-adapted |
| 4 | same-format rejected | `convertTextThrowsForIdenticalFormats` :42–48 | `"Input and output formats must differ"` :47 | GTest `SameFormatRejected` (cpp:58) |
| 5 | same-format case-insensitive | `convertTextThrowsForIdenticalFormatsCaseInsensitive` :50–56 | same message :55 | contract item 5 |
| 6 | stub csv→json | `convertTextThrowsNotImplementedForCsvToJson` :58–64 | `"Conversion from csv to json is not implemented"` :63 | contract item 6 |
| 7 | stub json→csv | `convertTextThrowsNotImplementedForJsonToCsv` :66–72 | `"Conversion from json to csv is not implemented"` :71 | contract item 7 |
| 8 | stub yaml→json | `convertTextThrowsNotImplementedForYamlToJson` :74–80 | `"Conversion from yaml to json is not implemented"` :79 | contract item 8 |
| 9 | stub yaml→csv | `convertTextThrowsNotImplementedForYamlToCsv` :82–88 | `"Conversion from yaml to csv is not implemented"` :87 | contract item 9 |
| 10 | convertFile, no extension | `convertFileThrowsForInputPathWithoutExtension` :90–99 | `"File has no extension: " + input` :98 (full path pinned) | converter.cpp:232 |
| 11 | convertFile, missing input | `convertFileThrowsForMissingInputFile` :101–110 | `"Cannot open input file: " + input` :109 | converter.cpp:241 |
| 12 | valid input, output NOT created | `convertFileFailsBeforeCreatingOutputFile` :112–123 | exact stub message :121 **plus** `assertFalse(Files.exists(output))` :122 | contract item 12 |
| 13 | (optional) extension check ordering | `convertFileChecksExtensionBeforeReadingInput` :126–134 | `"File has no extension: " + input` :133 — input also nonexistent, so this message is only reachable if the extension check precedes `readInput` | contract item 13 |

Message literals were cross-checked against the production sources and match verbatim:
`Converter.java:19` ("Input and output formats must differ"), :21 (`"Conversion from <in> to <out> is not
implemented"`, normalized names), :41 (`"Unsupported file extension: " + ext`), :49 (`"File has no
extension: " + path`), :58 (`"Cannot open input file: " + path`).

Real-conversion GTest cases (CsvToJson, CsvQuotedComma, CsvEscapedQuote, JsonToYaml, YamlToJson,
MalformedJsonRejected, EmptyCsvProducesEmptyArray, CsvWrongColumnCount, NestedObjectCannotExportToCsv) are
intentionally NOT ported — confirmed by design per plan (T2 inputs) and Q1 answer; all conversions are stubs,
so porting them would be impossible without adding forbidden dependencies. Correct scope.

## 2. Do the tests actually test? (mutation check)

No mocks exist anywhere — tests exercise the real static `Converter` against the real filesystem via `@TempDir`.
Every test stays green only if the production logic is correct; each would fail under a plausible mutation:

- Item 2: if `normalizeExt` skipped `toLowerCase` → message would contain `.XML`; if it skipped the dot-strip →
  `.xml` (`Converter.java:33-36`). Both mutations flip the `assertEquals` at :31. The single input `".XML"`
  genuinely pins both transformations (matching contract item 2 verbatim).
- Item 3: if the `yml→yaml` alias (`Converter.java:37-39`) were removed → `"Unsupported file extension: yml"`
  ≠ asserted message at :39. The alias is proven, not assumed.
- Item 5: if format equality were compared before lowercasing → `"JSON"` ≠ `"json"` → stub message instead of
  :55 → fails. Case-insensitivity through normalization is genuinely pinned.
- Item 12: if `convertFile` opened the output writer before `convertText` (reordering `Converter.java:27-29`),
  `Files.newBufferedWriter` would create an empty output file and `assertFalse(Files.exists(output))` at :122
  would fail. This is the strongest test in the file and it is a real behavioral guarantee, not a tautology.
- Item 13: if `readInput` ran before the extension check, the missing input would yield
  `"Cannot open input file: …"` ≠ :133 → fails. Ordering proven.
- Items 10/11: message bodies embed the full input path (`:98`, `:109`), so the tests also pin that the *right*
  path is reported, not just any exception.

No hollow/tautological assertions found. The setup matches the real dependency contract (path strings passed to
`Files.readString`/`newBufferedWriter` semantics; `@TempDir` guarantees a clean directory per test).

## 3. Rubric scan (test-rules + reviewer checklist)

- **Comments / AAA markers:** none. `grep '//|/\*|Given|When|Then'` over the test file → no matches. Clean.
- **Real behavior asserted:** yes (section 2); every test ends in an exact-value assertion.
- **Mocks:** none used, none needed; no Mockito imports; `any()`/`eq()` absent by construction.
- **Names:** method-under-test prefix (`convertText…`/`convertFile…`) + trigger + outcome (e.g.
  `convertFileFailsBeforeCreatingOutputFile` :113, `convertFileChecksExtensionBeforeReadingInput` :126).
  Descriptive camelCase, no `@DisplayName` — per explicit contract override (see section 5).
- **Magic literals:** all local to their one-off tests; nothing shared → no `static final` needed. `@TempDir
  Path tempDir` :15–16 is a genuine shared fixture used by 4 tests (items 10–13) — correct scope.
- **Parameterization:** items 6–9 are structurally identical and would normally be a `@ParameterizedTest`;
  the contract mandates plain `@Test` 1:1 with the GTest case mapping (see section 5). Not a violation.
- **Single-use locals:** none; every local (`input`, `output`, `ex`) is used ≥ 2×.
- **Redundant verifies/asserts:** none; no redundant `assertFalse` beyond the mandated item-12 one.
- **Test order:** new file; tests appear in matrix order 1→13, matching the contract sequence.
- **Out of scope:** nothing touches `Main`/`System.exit`. Confirmed.

## 4. Dependency / infra constraints

- Imports are exactly: `org.junit.jupiter.api.Test`, `org.junit.jupiter.api.io.TempDir`, `java.nio.file.Files`,
  `java.nio.file.Path`, and static `org.junit.jupiter.api.Assertions.{assertEquals, assertFalse, assertThrows}`
  (ConverterTest.java:3–11). Pure JDK + JUnit Jupiter — nothing else.
- **AssertJ is NOT used**: `grep -i 'assertj|assertThat'` over all of `data-converter/src` → no matches, even
  though `build.gradle.kts:18` still declares it (kept by human decision, forbidden for new usage). Compliant.
- `data-converter/build.gradle.kts` current content (:15–22) contains only the pre-existing deps
  (log4j-slf4j, assertj, junit-bom/jupiter/launcher) + toolchain 25 (:9–13) — no new dependencies. Caveat: git
  byte-diff against HEAD was impossible (shell denied), so "untouched" is verified at content level (deps
  block exactly matches the human amendment's inventory), not by diff.
- `data-converter/src/test` contains exactly one file: `ConverterTest.java` (glob confirmed) → tests=13 is
  consistent with 13 `@Test` methods counted in the file.

## 5. Rule-file conflicts — resolved in favor of the task contract (documented, not violations)

`.opencode/rules/test-rules.md` generically requires `@Nested` classes per production method, `@DisplayName` on
every test, and parameterization of duplicated cases. The plan contract (T2 inputs) **explicitly
overrides** all three for this module: package-private flat class, plain `@Test`, descriptive camelCase, no
`@DisplayName`, following the neighbor convention (`todo-app/.../TaskTest.java:14-21`), with a 1:1 mapping to
the GTest case list. The task contract is the human-specified, more specific rule; the generic file yields.
Result: style findings would be false positives here.

## 6. Non-blocking observations (no action required)

1. `@TempDir` + `lastIndexOf('.')` interplay: items 10/13 resolve `tempDir.resolve("input-no-extension")` and
   pin "File has no extension" — correct, because the production contract (converter.cpp:232 parity, plan
   3a) takes the extension from the **whole path string**. Fragile only on machines whose temp path contains a
   dot (e.g. user `john.doe`); current environment is dot-free (writer run green). Inherent to the production
   contract, not a test defect.
2. Item 2 (".XML") cannot distinguish "strip one leading dot" from "strip all dots" (only one dot present).
   The contract's matrix item 2 specifies exactly this input, so coverage matches the mandate; a `..csv` case
   would be a future strengthening if the "one dot" semantics ever becomes load-bearing.
3. Items 6–9 could collapse into one parameterized test under the generic repo rules; kept flat per contract
   (section 5).

## Verdict

**PASS** — all 13 matrix items present with exact-message assertions, real behavioral proofs (including the
fail-before-write and check-ordering guarantees), zero comments, zero new dependencies, contract-mandated style
followed. No fixes required.
