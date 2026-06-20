# Phase 7 Quality and Thesis Materials Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Turn the completed backend slices into repeatable verification assets and thesis-ready evidence for the first-version AI knowledge ticket system.

**Architecture:** Keep Phase 7 focused on quality and materials, not new business scope. Add a reusable smoke script for live backend verification, a structured RAG evaluation dataset with a lightweight validator, and concise documents that map implementation evidence to the first-version acceptance criteria and demo flow.

**Tech Stack:** Java 21, Maven/JUnit 5, POSIX shell, curl, Node.js built-ins for JSON parsing, Markdown documentation.

---

## File Structure

```text
tools/smoke/phase7-backend-smoke.sh
docs/evaluation/rag-evaluation-set.json
docs/evaluation/rag-evaluation-set.md
docs/acceptance/v1-acceptance-checklist.md
docs/demo/v1-demo-runbook.md
docs/spikes/phase-7-quality-and-thesis-materials.md
backend/src/test/java/com/example/aiticket/docs/RagEvaluationSetTest.java
backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java
docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md
docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md
沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md
```

## Task 1: Repeatable Backend Smoke Script ⭐

**Files:**
- Create: `tools/smoke/phase7-backend-smoke.sh`
- Create: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`

- [x] **Step 1: Write a failing documentation coverage test**

Create `DocumentationCoverageTest` with:

```java
package com.example.aiticket.docs;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class DocumentationCoverageTest {
    @Test
    void phase7SmokeScriptCoversCoreBackendEndpointsWithoutPrintingTokens() throws Exception {
        Path scriptPath = Path.of("../tools/smoke/phase7-backend-smoke.sh");
        assertThat(scriptPath).exists();
        String script = Files.readString(scriptPath);

        assertThat(script).contains("/api/auth/login");
        assertThat(script).contains("/api/auth/me");
        assertThat(script).contains("/api/knowledge/documents/text");
        assertThat(script).contains("/api/knowledge/search");
        assertThat(script).contains("/api/ai/chat/ask");
        assertThat(script).contains("/api/tickets/from-ai-session");
        assertThat(script).contains("/api/admin/statistics/overview");
        assertThat(script).contains("/api/admin/users");
        assertThat(script).contains("token:redacted");
        assertThat(script).doesNotContain("echo \"$ADMIN_TOKEN\"");
        assertThat(script).doesNotContain("echo \"$USER_TOKEN\"");
    }
}
```

- [x] **Step 2: Run the test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest test
```

Expected: fail because the script and test do not exist yet.

- [x] **Step 3: Create the smoke script**

Create `tools/smoke/phase7-backend-smoke.sh`:

```bash
#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://127.0.0.1:8080}"
ADMIN_USERNAME="${ADMIN_USERNAME:-admin}"
USER_USERNAME="${USER_USERNAME:-user}"
AGENT_USERNAME="${AGENT_USERNAME:-agent}"
PASSWORD="${PASSWORD:-Admin_123456}"
TMP_DIR="${TMPDIR:-/tmp}"
BODY_FILE="$TMP_DIR/phase7-smoke-response.json"

json_get() {
  node -e 'const fs=require("fs"); const path=process.argv[1]; const expr=process.argv[2]; const j=JSON.parse(fs.readFileSync(path,"utf8")); const v=expr.split(".").reduce((o,k)=>o && o[k], j); if (v === undefined || v === null) process.exit(2); if (typeof v === "object") console.log(JSON.stringify(v)); else console.log(v);' "$1" "$2"
}

shape() {
  node -e 'const fs=require("fs"); const s=fs.readFileSync(process.argv[1],"utf8"); let j; try { j=JSON.parse(s); } catch { console.log("non-json"); process.exit(0); } const d=j.data; if (Array.isArray(d)) console.log(`array(${d.length})`); else if (d && typeof d === "object") console.log(Object.keys(d).slice(0,10).join(",")); else console.log(typeof d);' "$1"
}

request() {
  local method="$1"
  local path="$2"
  local token="${3:-}"
  local body="${4:-}"
  local code
  if [[ -n "$token" && -n "$body" ]]; then
    code=$(curl -sS -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path" -H "Authorization: Bearer $token" -H 'Content-Type: application/json' -d "$body")
  elif [[ -n "$token" ]]; then
    code=$(curl -sS -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path" -H "Authorization: Bearer $token")
  elif [[ -n "$body" ]]; then
    code=$(curl -sS -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path" -H 'Content-Type: application/json' -d "$body")
  else
    code=$(curl -sS -o "$BODY_FILE" -w '%{http_code}' -X "$method" "$BASE_URL$path")
  fi
  printf '%s' "$code"
}

login() {
  local username="$1"
  local code
  code=$(request POST /api/auth/login "" "{\"username\":\"$username\",\"password\":\"$PASSWORD\"}")
  if [[ "$code" != "200" ]]; then
    printf 'login failed for %s with status %s\n' "$username" "$code" >&2
    cat "$BODY_FILE" >&2
    exit 1
  fi
  json_get "$BODY_FILE" data.accessToken
}

check() {
  local name="$1"
  local expected="$2"
  local method="$3"
  local path="$4"
  local token="${5:-}"
  local body="${6:-}"
  local code
  code=$(request "$method" "$path" "$token" "$body")
  local response_shape
  response_shape=$(shape "$BODY_FILE")
  printf '%s %s %s\n' "$name" "$code" "$response_shape"
  if [[ "$code" != "$expected" ]]; then
    printf 'expected %s for %s, got %s\n' "$expected" "$name" "$code" >&2
    cat "$BODY_FILE" >&2
    exit 1
  fi
}

ADMIN_TOKEN=$(login "$ADMIN_USERNAME")
USER_TOKEN=$(login "$USER_USERNAME")
AGENT_TOKEN=$(login "$AGENT_USERNAME")

printf 'adminLogin 200 token:redacted\n'
printf 'userLogin 200 token:redacted\n'
printf 'agentLogin 200 token:redacted\n'

check authMe 200 GET /api/auth/me "$ADMIN_TOKEN"
check createTextDocument 200 POST /api/knowledge/documents/text "$ADMIN_TOKEN" '{"title":"Phase 7 Smoke FAQ","content":"Phase 7 smoke verification password reset answer: users can reset a forgotten password from the login page by selecting forgot password and following the verification flow.","categoryId":1}'
DOCUMENT_ID=$(json_get "$BODY_FILE" data.id)
check knowledgeSearch 200 POST /api/knowledge/search "$USER_TOKEN" '{"query":"How do I reset a forgotten password?","topK":3}'
check ragAsk 200 POST /api/ai/chat/ask "$USER_TOKEN" '{"question":"How do I reset a forgotten password?"}'
SESSION_ID=$(json_get "$BODY_FILE" data.sessionId)
check createTicket 200 POST /api/tickets/from-ai-session "$USER_TOKEN" "{\"sessionId\":$SESSION_ID,\"title\":\"Phase 7 smoke ticket\",\"description\":\"Need manual confirmation after smoke ask\",\"categoryId\":1,\"priority\":\"MEDIUM\"}"
TICKET_ID=$(json_get "$BODY_FILE" data.id)
check assignTicket 200 POST "/api/tickets/$TICKET_ID/assign" "$ADMIN_TOKEN" '{"assigneeId":3,"comment":"Phase 7 smoke assignment"}'
check myTickets 200 GET /api/tickets/my "$USER_TOKEN"
check assignedTickets 200 GET /api/tickets/assigned "$AGENT_TOKEN"
check adminOverview 200 GET /api/admin/statistics/overview "$ADMIN_TOKEN"
check adminUsers 200 GET /api/admin/users "$ADMIN_TOKEN"
check anonymousAdminOverview 401 GET /api/admin/statistics/overview
check userAdminUsersForbidden 403 GET /api/admin/users "$USER_TOKEN"

printf 'phase7SmokeDocumentId %s\n' "$DOCUMENT_ID"
printf 'phase7SmokeTicketId %s\n' "$TICKET_ID"
```

- [x] **Step 4: Make the script executable and verify syntax**

```bash
chmod +x tools/smoke/phase7-backend-smoke.sh
bash -n tools/smoke/phase7-backend-smoke.sh
```

Expected: both commands succeed.

- [x] **Step 5: Run focused test to verify GREEN**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest test
```

- [x] **Step 6: Commit Task 1**

```bash
git add tools/smoke/phase7-backend-smoke.sh backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md
git commit -m "test: add phase 7 backend smoke script"
```

## Task 2: RAG Evaluation Dataset ⭐

**Files:**
- Create: `docs/evaluation/rag-evaluation-set.json`
- Create: `docs/evaluation/rag-evaluation-set.md`
- Create: `backend/src/test/java/com/example/aiticket/docs/RagEvaluationSetTest.java`

- [x] **Step 1: Write the failing dataset structure test**

Create `RagEvaluationSetTest` that reads `../docs/evaluation/rag-evaluation-set.json` and asserts:

```text
20 evaluation cases exist
each case has id, category, question, expectedKeywords, expectedSourceHint, shouldTransfer
at least 5 cases have shouldTransfer=true
at least 5 distinct categories exist
```

- [x] **Step 2: Run the test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=RagEvaluationSetTest test
```

Expected: fail because the dataset does not exist.

- [x] **Step 3: Create the JSON evaluation set**

Create 20 Chinese enterprise-service questions across password/account, reimbursement, device, network, permission, and unknown/out-of-scope categories. Each object must include:

```json
{
  "id": "RAG-001",
  "category": "账号与登录",
  "question": "忘记密码后应该如何重置？",
  "expectedKeywords": ["忘记密码", "验证", "重置"],
  "expectedSourceHint": "账号登录 FAQ 或 IT 支持手册",
  "shouldTransfer": false
}
```

- [x] **Step 4: Create the Markdown evaluation guide**

Create `docs/evaluation/rag-evaluation-set.md` documenting:

```text
purpose
case fields
manual scoring rules for retrieval hit, answer usefulness, and mistaken transfer
how to report aggregate metrics
```

- [x] **Step 5: Run focused dataset test to verify GREEN**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=RagEvaluationSetTest test
```

- [x] **Step 6: Commit Task 2**

```bash
git add docs/evaluation backend/src/test/java/com/example/aiticket/docs/RagEvaluationSetTest.java docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md
git commit -m "docs: add rag evaluation set"
```

## Task 3: Acceptance Matrix and Demo Runbook ⭐

**Files:**
- Create: `docs/acceptance/v1-acceptance-checklist.md`
- Create: `docs/demo/v1-demo-runbook.md`
- Modify: `backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java`

- [x] **Step 1: Extend documentation coverage test**

Add assertions that these files exist and contain all major module names:

```text
docs/acceptance/v1-acceptance-checklist.md
docs/demo/v1-demo-runbook.md
RBAC
知识库
RAG
工单
统计
Redis
```

- [x] **Step 2: Run the test to verify RED**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest test
```

Expected: fail because acceptance and demo docs do not exist.

- [x] **Step 3: Create the acceptance checklist**

Create `docs/acceptance/v1-acceptance-checklist.md` with a table mapping the 14 acceptance criteria from the project plan to:

```text
implemented backend evidence
automated test evidence
live smoke or manual demo evidence
remaining frontend/deferred note
```

- [x] **Step 4: Create the demo runbook**

Create `docs/demo/v1-demo-runbook.md` with a 10-15 minute demo sequence:

```text
environment startup
admin login and knowledge text creation
user RAG question
manual transfer to ticket
admin assignment
agent processing
user comment / close
admin statistics overview
RBAC denial checks
```

- [x] **Step 5: Run focused documentation test to verify GREEN**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest test
```

- [x] **Step 6: Commit Task 3**

```bash
git add docs/acceptance docs/demo backend/src/test/java/com/example/aiticket/docs/DocumentationCoverageTest.java docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md
git commit -m "docs: add acceptance and demo runbooks"
```

## Task 4: Phase 7 Verification Report and Project Progress ⭐

**Files:**
- Create: `docs/spikes/phase-7-quality-and-thesis-materials.md`
- Modify: `docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
- Modify: `沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md`
- Modify: `docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md`

- [x] **Step 1: Run focused docs tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo -Dtest=DocumentationCoverageTest,RagEvaluationSetTest test
```

- [x] **Step 2: Run all backend tests**

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dmaven.repo.local=/Users/xianghuaifeng/Documents/毕业设计/.worktrees/knowledge-live-verification/.m2repo test
```

- [x] **Step 3: Optionally run live smoke**

If Oracle, Redis, backend, and model/mock providers are available:

```bash
BASE_URL=http://127.0.0.1:8080 tools/smoke/phase7-backend-smoke.sh
```

Record sanitized output. If providers are not available, document why it was skipped and rely on the latest Phase 6 smoke plus automated test evidence.

- [x] **Step 4: Create Phase 7 spike report**

Write `docs/spikes/phase-7-quality-and-thesis-materials.md` with:

```text
automated test results
smoke script syntax/runtime result
RAG evaluation set summary
acceptance/demo document summary
remaining frontend and thesis writing scope
```

- [x] **Step 5: Update project plan progress**

In both project plan copies, update section `12.0 当前实现进度` so Phase 7 testing/materials are marked completed for backend evidence, with thesis prose and frontend UI still noted as follow-up integration work.

- [x] **Step 6: Final diff and commit**

```bash
git diff --check
git status --short --branch
git add docs/spikes/phase-7-quality-and-thesis-materials.md docs/superpowers/plans/2026-06-20-phase-7-quality-and-thesis-materials-implementation-plan.md docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md 沟通材料/2026-06-19-ai-knowledge-ticket-v1-project-plan.md
git commit -m "docs: verify phase 7 quality materials"
```

## Current Execution Note

Phase 7 starts after Phase 6 backend admin/statistics APIs are complete and live-smoke verified. Keep this phase evidence-focused: do not add new product capabilities unless a verification gap proves one is necessary.
