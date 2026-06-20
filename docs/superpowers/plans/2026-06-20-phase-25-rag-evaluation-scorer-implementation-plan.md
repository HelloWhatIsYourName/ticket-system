# Phase 25 RAG Evaluation Scorer Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make live RAG evaluation scoring explainable, testable, and aligned with actual citation evidence.

**Architecture:** Extract the inline scoring logic from `tools/smoke/phase23-run-rag-evaluation.sh` into a CommonJS scorer module. Keep the smoke script responsible for login/API calls, while the scorer owns retrieval-hit, answer-usefulness, transfer-rate, and summary calculations.

**Tech Stack:** Bash, Node.js CommonJS, Java documentation coverage tests, existing live RAG smoke scripts.

---

## File Structure

- Create: `tools/smoke/rag-evaluation-scorer.cjs`
  - Provides reusable scoring helpers for live RAG evaluation.
- Create: `tools/smoke/rag-evaluation-scorer.test.cjs`
  - Node self-test for source hint aliases and metric summary behavior.
- Modify: `tools/smoke/phase23-run-rag-evaluation.sh`
  - Delegates scoring to the scorer module and keeps API execution only.
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`
  - Requires scorer module/self-test coverage and script delegation.
- Modify: `docs/evaluation/rag-live-evaluation-report.md`
  - Updates post-scorer live metrics.
- Create: `docs/superpowers/plans/2026-06-20-phase-25-rag-evaluation-scorer-implementation-plan.md`
  - Tracks Phase 25 execution.

---

### Task 1: Add RED Scorer Coverage

**Files:**
- Create: `tools/smoke/rag-evaluation-scorer.test.cjs`
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`

- [x] ⭐ **Step 1: Write failing Node self-test**

Create a self-test that imports `./rag-evaluation-scorer.cjs` and asserts:
- source hint `知识库不应包含个人实时行程` matches citation evidence from `DEMO-KB-016 敏感个人信息处理边界`;
- source hint `薪酬问题需要 HR 或财务人工核查` matches the same sensitive personal information boundary;
- source hint `知识库不应回答未公开人事预测` matches `DEMO-KB-019 未公开人事信息处理边界`;
- summary rates are computed from scored cases.

- [x] ⭐ **Step 2: Extend documentation coverage**

Update `DocumentationCoverageTest.phase23DemoCorpusAndRagEvaluationScriptsAreRepeatableAndSecretSafe` to assert:
- `tools/smoke/rag-evaluation-scorer.cjs` exists;
- `tools/smoke/rag-evaluation-scorer.test.cjs` exists;
- `phase23-run-rag-evaluation.sh` contains `SCORER_PATH`;
- scorer module contains `sourceHintAliases`, `retrievalHit`, and `summarizeResults`.

- [x] ⭐ **Step 3: Run tests to verify RED**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: FAIL because the scorer module does not exist yet.

---

### Task 2: Extract Scorer Module

**Files:**
- Create: `tools/smoke/rag-evaluation-scorer.cjs`
- Modify: `tools/smoke/phase23-run-rag-evaluation.sh`

- [x] ⭐ **Step 1: Implement scorer module**

Implement these exports:
- `normalize(value)`
- `sourceHintAliases(expectedSourceHint)`
- `retrievalHit(answer, caseItem)`
- `usefulAnswer(answer, caseItem)`
- `scoreCase(answer, caseItem, status)`
- `summarizeResults(results)`

- [x] ⭐ **Step 2: Wire evaluator script**

Add:

```bash
SCORER_PATH="${SCORER_PATH:-$REPO_ROOT/tools/smoke/rag-evaluation-scorer.cjs}"
export SCORER_PATH
```

Then require the scorer inside the Node body and remove duplicated inline scoring functions.

- [x] ⭐ **Step 3: Verify GREEN**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
bash -n tools/smoke/phase23-run-rag-evaluation.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=DocumentationCoverageTest test
```

Expected: PASS.

---

### Task 3: Rerun Live Evaluation and Report Metrics

**Files:**
- Modify: `docs/evaluation/rag-live-evaluation-report.md`

- [x] ⭐ **Step 1: Run live evaluation**

Run:

```bash
tools/smoke/phase23-run-rag-evaluation.sh
```

Expected: PASS. Retrieval hit rate should account for boundary-doc source aliases.

- [x] ⭐ **Step 2: Update report**

Update the metric summary and representative evidence to note Phase 25 scorer aliasing.

---

### Task 4: Final Verification and Commit

**Files:**
- Modify all Phase 25 files.

- [x] ⭐ **Step 1: Run full verification**

Run:

```bash
node tools/smoke/rag-evaluation-scorer.test.cjs
bash -n tools/smoke/phase23-run-rag-evaluation.sh
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Expected: PASS.

- [x] ⭐ **Step 2: Mark this plan complete**

Change each completed checkbox to `- [x] ⭐`.

- [x] ⭐ **Step 3: Commit**

Run:

```bash
git add tools/smoke/rag-evaluation-scorer.cjs tools/smoke/rag-evaluation-scorer.test.cjs tools/smoke/phase23-run-rag-evaluation.sh backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java docs/evaluation/rag-live-evaluation-report.md docs/superpowers/plans/2026-06-20-phase-25-rag-evaluation-scorer-implementation-plan.md
git commit -m "test: extract rag evaluation scorer"
```
