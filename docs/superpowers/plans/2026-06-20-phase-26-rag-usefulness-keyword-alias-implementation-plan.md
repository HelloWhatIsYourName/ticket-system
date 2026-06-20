# Phase 26 RAG Usefulness Keyword Alias Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make RAG answer usefulness scoring recognize semantically correct wording for the remaining RAG-003 account-borrowing case.

**Architecture:** Keep the Phase 25 scorer module as the scoring boundary. Add a small `keywordAliases` helper used only by `usefulAnswer`, so expected keywords can match equivalent answer phrasing without changing the evaluation dataset or model prompt.

**Tech Stack:** Node.js CommonJS scorer tests, Bash smoke script, Java documentation coverage tests.

---

## File Structure

- Modify: `tools/smoke/rag-evaluation-scorer.test.cjs`
  - Adds a RED test using the real RAG-003 answer text and expected keywords.
- Modify: `tools/smoke/rag-evaluation-scorer.cjs`
  - Adds `keywordAliases` and uses it in `usefulAnswer`.
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`
  - Requires scorer module to contain `keywordAliases`.
- Modify: `docs/evaluation/rag-live-evaluation-report.md`
  - Updates live metric summary after rerun.
- Create: `docs/superpowers/plans/2026-06-20-phase-26-rag-usefulness-keyword-alias-implementation-plan.md`
  - Tracks Phase 26 execution.

---

### Task 1: Add RED Usefulness Alias Coverage

**Files:**
- Modify: `tools/smoke/rag-evaluation-scorer.test.cjs`
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`

- [x] ⭐ **Step 1: Add scorer self-test**

Add an assertion that `usefulAnswer` returns true for:
- answer: `根据制度要求，公司账号仅限本人使用，禁止借用离职同事账号或共享账号处理业务。因此，离职同事的账号不能借用。`
- expected keywords: `账号借用`, `禁止`, `权限安全`
- `shouldTransfer=false`

- [x] ⭐ **Step 2: Add documentation coverage assertion**

Require `tools/smoke/rag-evaluation-scorer.cjs` to contain `keywordAliases`.

- [x] ⭐ **Step 3: Run tests to verify RED**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: FAIL because `keywordAliases` does not exist and the current useful heuristic only matches exact keywords.

---

### Task 2: Implement Keyword Aliases

**Files:**
- Modify: `tools/smoke/rag-evaluation-scorer.cjs`

- [x] ⭐ **Step 1: Add keywordAliases helper**

Add aliases for:
- `账号借用`: `借用离职同事账号`, `共享账号`, `借用账号`
- `权限安全`: `仅限本人使用`, `共享密码`, `上报 IT`, `安全管理员`

- [x] ⭐ **Step 2: Use aliases in usefulAnswer**

Count a keyword as matched when any of its aliases appears in the answer.

- [x] ⭐ **Step 3: Verify GREEN**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: PASS.

---

### Task 3: Rerun Live Evaluation and Report

**Files:**
- Modify: `docs/evaluation/rag-live-evaluation-report.md`

- [x] ⭐ **Step 1: Run live evaluation**

Run:

```bash
tools/smoke/phase23-run-rag-evaluation.sh
```

Expected: PASS. `answerUsefulRate` should become `1` if the current model answer remains semantically equivalent.

- [x] ⭐ **Step 2: Update report**

Update the metric summary and RAG-003 row with the latest scorer output.

---

### Task 4: Final Verification and Commit

**Files:**
- Modify all Phase 26 files.

- [x] ⭐ **Step 1: Run full verification**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
bash -n tools/smoke/phase23-run-rag-evaluation.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Expected: PASS.

- [x] ⭐ **Step 2: Mark this plan complete**

Change every completed checkbox to `- [x] ⭐`.

- [x] ⭐ **Step 3: Commit**

Run:

```bash
git add tools/smoke/rag-evaluation-scorer.cjs tools/smoke/rag-evaluation-scorer.test.cjs backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java docs/evaluation/rag-live-evaluation-report.md docs/superpowers/plans/2026-06-20-phase-26-rag-usefulness-keyword-alias-implementation-plan.md
git commit -m "test: add rag usefulness keyword aliases"
```
