---
description: Standalone review of the working-tree diff (default) or a commit / range argument — production AND tests in one pass, each judged against its own rules; writes one report under opencode-reviews/. Read-only — never triggers a fix.
agent: code-reviewer
subtask: true
---

Do a **standalone, read-only review of a diff** — covering **both production and test** changes in a single pass. This
is NOT a pipeline gate — nothing happens after it except the human reading the report.

**Optional ref:** $ARGUMENTS

**Working-tree diff (default target):**
!`git add -N .`
!`git diff HEAD`

- **No ref given** → review **only the diff shown above** — the current un-committed work. (`git add -N` only marks new
  files as intent-to-add so they appear in the diff; it does not stage their contents, so this stays a read-only
  review.)
- **Ref given** → ignore the diff above; review `git show $ARGUMENTS` (one commit) or `git diff $ARGUMENTS` (range /
  branch) yourself.

Rules for this command:

- **Review ONLY the changed (`+` / `-`) lines.** Read nearby code or callers solely to judge a changed line — never
  review, list, or comment on unchanged code, other files, or the wider project.
- **Judge each changed file against the RIGHT rules** (load only the rule files the diff actually needs):
    - a file under **`src/main`** → `.opencode/rules/java-rules.md`;
    - a **`.java` test** under **`src/test`** → `.opencode/rules/test-rules.md`;
- Write **ONE** report to `opencode-reviews/<subject>.review.md` — group findings by file and note which rules each was
  judged against. Give a **single overall PASS / FAIL** verdict in your reply (FAIL if any area fails).
- **Then STOP.** Do not dispatch anyone, do not hand anything to `coder` / `tester`, do not start a fix. A FAIL is
  information for the human — they decide what happens next, not you.
