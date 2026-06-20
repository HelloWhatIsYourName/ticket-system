# Phase 24 RAG Transfer Policy Tuning Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Reduce missed transfer suggestions for sensitive or human-required RAG questions discovered by the Phase 23 live evaluation.

**Architecture:** Keep the existing `RagAnswerPolicy` as the single decision point, but pass the user question into it so policy can combine query intent, retrieval evidence, similarity, and model self-assessment. Add a narrow, explainable manual-boundary heuristic for privacy, salary, non-public HR, contract amount changes, and production incident recovery while preserving automatic answers for normal FAQ cases.

**Tech Stack:** Java 21, JUnit 5, AssertJ, Spring Boot service layer, existing `RagAnswerPolicy`.

---

## File Structure

- Modify: `backend/src/test/java/com/example/aiticket/ai/rag/service/RagAnswerPolicyTest.java`
  - Adds RED tests for sensitive/manual boundary transfer decisions and regression coverage for normal FAQ answers.
- Modify: `backend/src/main/java/com/example/aiticket/ai/rag/service/RagAnswerPolicy.java`
  - Adds question-aware policy overload and manual-boundary detection helper.
- Modify: `backend/src/main/java/com/example/aiticket/ai/rag/service/RagChatService.java`
  - Passes the trimmed user question into `RagAnswerPolicy`.
- Modify: `docs/evaluation/rag-live-evaluation-report.md`
  - Records post-tuning live evaluation metrics when rerun.
- Create: `docs/superpowers/plans/2026-06-20-phase-24-rag-transfer-policy-tuning-implementation-plan.md`
  - Tracks Phase 24 execution.

---

### Task 1: Add Transfer Policy Regression Tests

**Files:**
- Modify: `backend/src/test/java/com/example/aiticket/ai/rag/service/RagAnswerPolicyTest.java`

- [x] ⭐ **Step 1: Write failing tests**

Add tests that assert:
- A personal itinerary question with `敏感个人信息处理边界` retrieval suggests transfer.
- A contract amount change question with `合同审批和销售授权制度` retrieval suggests transfer.
- A normal password reset question still answers automatically.

- [x] ⭐ **Step 2: Run test to verify it fails**

Run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=RagAnswerPolicyTest test
```

Expected: FAIL because `RagAnswerPolicy` cannot inspect the user question yet.

---

### Task 2: Implement Question-Aware Manual Boundary Policy

**Files:**
- Modify: `backend/src/main/java/com/example/aiticket/ai/rag/service/RagAnswerPolicy.java`
- Modify: `backend/src/main/java/com/example/aiticket/ai/rag/service/RagChatService.java`

- [x] ⭐ **Step 1: Add question-aware overload**

Add `decide(List<KnowledgeSearchResult>, ChatResult, double, String)` and keep the existing three-argument method delegating to it with `null` question.

- [x] ⭐ **Step 2: Add manual-boundary heuristic**

Return transfer when the question contains sensitive/manual intent and retrieval evidence contains matching boundary content:
- privacy / itinerary: `个人行程`, `在哪里开会`, `敏感个人信息`
- salary / HR: `工资`, `薪酬`, `裁员`, `未公开人事`
- contract amount: `合同金额`, `临时改`, `直接答应`, `合同审批`, `销售授权`
- production incident: `生产数据库`, `误删`, `生产事故`, `应急预案`

- [x] ⭐ **Step 3: Wire service call**

Change `RagChatService` to pass `question.trim()` into `answerPolicy.decide`.

- [x] ⭐ **Step 4: Run focused test**

Run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=RagAnswerPolicyTest,RagChatServiceTest test
```

Expected: PASS.

---

### Task 3: Rerun Evaluation Evidence

**Files:**
- Modify: `docs/evaluation/rag-live-evaluation-report.md`

- [x] ⭐ **Step 1: Run backend tests**

Run:

```bash
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Expected: PASS.

- [x] ⭐ **Step 2: Rerun live evaluation**

Run:

```bash
tools/smoke/phase23-run-rag-evaluation.sh
```

Expected: PASS. `missedTransferRate` should decrease from 1.00 for the five known transfer-required cases.

- [x] ⭐ **Step 3: Update report metrics**

Update `docs/evaluation/rag-live-evaluation-report.md` with the new metric summary and note Phase 24 policy tuning.

---

### Task 4: Mark Plan and Commit

**Files:**
- Modify: all Phase 24 files above.

- [x] ⭐ **Step 1: Mark this plan complete**

Change each completed checkbox to `- [x] ⭐`.

- [x] ⭐ **Step 2: Commit**

Run:

```bash
git add backend/src/test/java/com/example/aiticket/ai/rag/service/RagAnswerPolicyTest.java backend/src/main/java/com/example/aiticket/ai/rag/service/RagAnswerPolicy.java backend/src/main/java/com/example/aiticket/ai/rag/service/RagChatService.java docs/evaluation/rag-live-evaluation-report.md docs/superpowers/plans/2026-06-20-phase-24-rag-transfer-policy-tuning-implementation-plan.md
git commit -m "feat: tune rag transfer policy"
```
