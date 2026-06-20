# Phase 18 Knowledge File Upload Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

> **Progress rule for this project:** after a full task is implemented, verified, reviewed, and committed, append `⭐` to that task heading line and keep the step checkboxes accurate.

**Goal:** Add V1-ready knowledge file upload for small text/Markdown documents with type whitelist and size validation, reusing the existing ingestion and vectorization pipeline.

**Architecture:** Keep binary parsing out of V1 scope and support `.txt`, `.md`, and `.markdown` as UTF-8 text files. Backend multipart upload validates filename, extension, non-empty content, and 200 KB size limit before creating a `kb_document` row and calling the same synchronous `KnowledgeIngestionService.ingestText()` path used by manual text entry. Frontend adds a small upload entry in `/app/knowledge` while preserving the existing text form and retrieval test.

**Tech Stack:** Spring MVC multipart, Java 21, JUnit 5, AssertJ, Vue 3, TypeScript, Axios, Vitest.

---

## File Structure

```text
backend/src/main/java/com/example/aiticket/knowledge/mapper/KnowledgeDocumentMapper.java
backend/src/main/java/com/example/aiticket/knowledge/service/KnowledgeDocumentService.java
backend/src/main/java/com/example/aiticket/knowledge/web/KnowledgeDocumentController.java
backend/src/main/resources/mapper/KnowledgeDocumentMapper.xml
backend/src/test/java/com/example/aiticket/knowledge/web/KnowledgeDocumentControllerTest.java
frontend/src/api/knowledge.ts
frontend/src/api/knowledge.spec.ts
frontend/src/views/knowledge/KnowledgeBaseView.vue
frontend/src/views/knowledge/KnowledgeBaseView.spec.ts
docs/acceptance/v1-acceptance-checklist.md
docs/demo/v1-demo-runbook.md
docs/superpowers/plans/2026-06-20-phase-18-knowledge-file-upload-implementation-plan.md
```

## Task 1: Backend Multipart Upload ⭐

**Files:**
- Modify: `backend/src/main/java/com/example/aiticket/knowledge/mapper/KnowledgeDocumentMapper.java`
- Modify: `backend/src/main/java/com/example/aiticket/knowledge/service/KnowledgeDocumentService.java`
- Modify: `backend/src/main/java/com/example/aiticket/knowledge/web/KnowledgeDocumentController.java`
- Modify: `backend/src/main/resources/mapper/KnowledgeDocumentMapper.xml`
- Modify: `backend/src/test/java/com/example/aiticket/knowledge/web/KnowledgeDocumentControllerTest.java`

- [x] **Step 1: Write failing backend tests**

Add controller tests that:

1. Upload `policy.md` as `MockMultipartFile`, assert `KnowledgeIngestionService.ingestText()` receives the UTF-8 content and the response is successful.
2. Upload `policy.pdf`, assert `IllegalArgumentException` with message containing `unsupported`.
3. Upload a file larger than 200,000 bytes, assert `IllegalArgumentException` with message containing `too large`.

Also extend the PreAuthorize annotation test to cover the new upload method with `knowledge:document:upload`.

- [x] **Step 2: Run focused backend test to verify RED**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=KnowledgeDocumentControllerTest test
```

Expected: fail because `uploadDocument` and mapper support do not exist yet.

- [x] **Step 3: Implement mapper and service support**

Add `insertDocument(...)` to `KnowledgeDocumentMapper` and XML, including `file_name`, `file_type`, and `file_size`. Add `KnowledgeDocumentService.createDocument(...)` so both upload and future document sources can store explicit metadata.

- [x] **Step 4: Implement upload endpoint**

Add:

```java
@PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
@PreAuthorize("hasAuthority('knowledge:document:upload')")
public ApiResponse<DocumentResponse> uploadDocument(...)
```

Validate extension whitelist `.txt`, `.md`, `.markdown`, reject empty and oversized files, derive title from request title or filename stem, read UTF-8 content, then call ingestion and return the same document response style as `createTextDocument`.

- [x] **Step 5: Run focused backend test to verify GREEN**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=KnowledgeDocumentControllerTest test
```

Expected: pass.

## Task 2: Frontend Upload API and Workspace ⭐

**Files:**
- Modify: `frontend/src/api/knowledge.ts`
- Modify: `frontend/src/api/knowledge.spec.ts`
- Modify: `frontend/src/views/knowledge/KnowledgeBaseView.vue`
- Modify: `frontend/src/views/knowledge/KnowledgeBaseView.spec.ts`

- [x] **Step 1: Write failing frontend API and page tests**

Extend `knowledge.spec.ts` to assert `uploadKnowledgeDocument({ file, title, categoryId })` posts `FormData` to `/kb/documents/upload`.

Extend `KnowledgeBaseView.spec.ts` to mock `uploadKnowledgeDocument`, set a `.md` file on the file input, click the upload button, and assert the API is called with the file, title, and category ID.

- [x] **Step 2: Run focused frontend tests to verify RED**

Run:

```bash
cd frontend
npm run test -- src/api/knowledge.spec.ts src/views/knowledge/KnowledgeBaseView.spec.ts
```

Expected: fail because upload API and UI do not exist.

- [x] **Step 3: Implement upload API**

Add:

```ts
export interface UploadKnowledgeDocumentRequest {
  file: File
  title?: string
  categoryId?: number
}
```

`uploadKnowledgeDocument()` builds `FormData` with `file`, optional `title`, and optional `categoryId`, then posts to `/kb/documents/upload`.

- [x] **Step 4: Implement upload UI**

Add an upload block to the knowledge workspace with file input accepting `.txt,.md,.markdown`, an upload button, and a short status line. Reuse the existing title and category fields; after successful upload clear selected file/title/content and refresh document list.

- [x] **Step 5: Run focused frontend tests to verify GREEN**

Run:

```bash
cd frontend
npm run test -- src/api/knowledge.spec.ts src/views/knowledge/KnowledgeBaseView.spec.ts
```

Expected: pass.

## Task 3: Acceptance Documentation Sync ⭐

**Files:**
- Modify: `docs/acceptance/v1-acceptance-checklist.md`
- Modify: `docs/demo/v1-demo-runbook.md`

- [x] **Step 1: Update acceptance checklist**

Update rows 2 and 14 so multipart `.txt/.md` upload, whitelist, and 200 KB limit are described as implemented V1 evidence rather than remaining hardening.

- [x] **Step 2: Update demo runbook**

In the knowledge preparation section, mention that demo corpus items can be loaded either by text entry or by `.txt/.md` upload through `/app/knowledge`.

## Task 4: Full Verification and Plan Marking ⭐

**Files:**
- Modify: `docs/superpowers/plans/2026-06-20-phase-18-knowledge-file-upload-implementation-plan.md`

- [x] **Step 1: Run backend focused test**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn -Dtest=KnowledgeDocumentControllerTest test
```

Expected: pass.

- [x] **Step 2: Run frontend focused tests**

Run:

```bash
cd frontend
npm run test -- src/api/knowledge.spec.ts src/views/knowledge/KnowledgeBaseView.spec.ts
```

Expected: pass.

- [x] **Step 3: Run full backend tests**

Run:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH mvn test
```

Expected: pass.

- [x] **Step 4: Run frontend test suite**

Run:

```bash
cd frontend
npm run test
```

Expected: pass.

- [x] **Step 5: Run frontend production build**

Run:

```bash
cd frontend
npm run build
```

Expected: pass.

- [x] **Step 6: Mark completed plan tasks**

Append `⭐` to each completed task heading and change completed steps to `- [x]`.

- [x] **Step 7: Commit Phase 18 slice**

Run:

```bash
git add backend/src/main/java/com/example/aiticket/knowledge backend/src/main/resources/mapper/KnowledgeDocumentMapper.xml backend/src/test/java/com/example/aiticket/knowledge/web/KnowledgeDocumentControllerTest.java frontend/src/api/knowledge.ts frontend/src/api/knowledge.spec.ts frontend/src/views/knowledge/KnowledgeBaseView.vue frontend/src/views/knowledge/KnowledgeBaseView.spec.ts docs/acceptance/v1-acceptance-checklist.md docs/demo/v1-demo-runbook.md docs/superpowers/plans/2026-06-20-phase-18-knowledge-file-upload-implementation-plan.md
git commit -m "feat: add knowledge file upload"
```
