# Build commands (Gradle)

Universal Gradle usage. **No project-specific task or module names here** — these commands work on any Gradle project; discover the project's actual tasks rather than assuming.

## Always use the wrapper
- Run `./gradlew <task>` — the agents work in git-bash, so **always use `./gradlew`** (the `gradlew.bat` form is only for `cmd`, which the agents don't use). Never call a system `gradle`.
- **Always pass `--console=plain`** — mandatory, not optional. It strips gradle's progress-bar / ANSI spam so the tool output stays small and parseable; a large or messy gradle output is a known trigger for OpenCode's Ollama empty-reply glitch (#18423) and needlessly burns context. The daemon is fine for repeated runs; don't pass `--no-daemon` routinely.

## One build at a time — never run concurrent gradle
Gradle takes an **exclusive lock** on the project (`.gradle/` + `build/`). A second `./gradlew` started on the same project while another is still running **blocks or fails with lock-timeout errors**, and can corrupt the build cache. So:
- **One gradle invocation at a time per project** — let one finish before starting the next.
- This includes the **`spotless-on-edit` plugin**, which fires `./gradlew spotlessApply` on every `.java` write: don't kick off a manual build in the same breath as an edit. Within a single agent the tool calls run in sequence, so the write-triggered format finishes before your next command — just don't try to outrun it.
- The **orchestrator never dispatches two build-running agents (`coder` / `tester` / `automation-tester`) in parallel** for exactly this reason — each ends in a gradle run.

## Concurrent `git add` — the index lock
A separate, smaller lock: `git add` (auto-staging on every write, and the reviewers' `git add` before they diff) briefly locks `.git/index`. Two `git add` at the same instant — e.g. `code-reviewer` and `test-reviewer` run in parallel over one mixed prod+test change — can collide with `Unable to create '.git/index.lock': File exists`. Staging is best-effort (swallowed) and reviewers re-stage, so it self-heals; but if you parallelize the two reviewers and staging looks flaky, **serialize them** (production review, then test review).

## Discover, don't assume
- `./gradlew tasks --all` — list available tasks (find the real names; custom builds rename things).
- `./gradlew projects` — see the module layout in a multi-module build.
- Module-scoped task: `./gradlew :module:task` (e.g. `:app:test`).

## Common lifecycle
- `./gradlew build` — compile + test + checks + assemble (the full gate).
- `./gradlew assemble` — compile/package without tests.
- `./gradlew check` — tests + verification (no packaging).
- `./gradlew clean` — remove `build/` outputs.
- **`./gradlew :module:compileJava --console=plain`** — compile **production only, no tests** (`:module:compileTestJava` for test code). This is `coder`'s «targeted compile» verify — **not** `build` (runs the whole suite) and **not** `assemble` (also packages).

## Run tests narrowly (do NOT run the whole suite to check one thing)
- One class: `./gradlew test --tests 'com.example.FooTest'`
- One method: `./gradlew test --tests 'com.example.FooTest.methodName'`
- Pattern: `./gradlew test --tests '*Service*'`
- **Several classes → ONE invocation, not several:** `./gradlew test --tests 'a.FooTest' --tests 'a.BarTest' --console=plain`. **Never fire multiple `./gradlew` calls in one turn** — OpenCode may run them concurrently, and concurrent gradle builds collide on the project lock (see § One build at a time): the loser fails with a lock-timeout / «file is being used by another process» on Windows.
- Reports land in `build/reports/tests/test/index.html`; failing assertions print to the console with `--console=plain`.

## Formatting — automatic via the `spotless-on-edit` plugin
- The `spotless-on-edit` plugin runs `./gradlew spotlessApply` on every `.java` write, so your code always matches what the build gate **`spotlessCheck`** expects — you do NOT run it by hand, and you never hand-format.
- `./gradlew spotlessCheck` only verifies (the full build runs it); the plugin has already applied the fixes.

## Notes
- Prefer the narrowest task that proves your change (targeted `--tests` over full `build`).
- First invocation downloads Gradle + dependencies into `~/.gradle` (normal; not an "install").
