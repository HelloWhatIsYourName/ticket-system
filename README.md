# AI Knowledge Ticket System

An AI-powered knowledge-base question answering and ticket collaboration system for enterprise support scenarios.

## 1. API Configuration

The backend reads provider and infrastructure settings from environment variables. Do not commit API keys, JWTs, `.env` files, or local secret files.

### AI Chat Provider

The default chat provider is DeepSeek.

| Variable | Default | Description |
| --- | --- | --- |
| `AI_CHAT_BASE_URL` | `https://api.deepseek.com` | OpenAI-compatible chat API base URL |
| `AI_CHAT_API_KEY` | empty | Chat provider API key |
| `AI_CHAT_MODEL` | `deepseek-chat` | Chat model name |

### Embedding Provider

The default embedding provider is SiliconFlow.

| Variable | Default | Description |
| --- | --- | --- |
| `AI_EMBEDDING_BASE_URL` | `https://api.siliconflow.cn/v1` | OpenAI-compatible embedding API base URL |
| `AI_EMBEDDING_API_KEY` | empty | Embedding provider API key |
| `AI_EMBEDDING_MODEL` | `Qwen/Qwen3-Embedding-8B` | Embedding model name |
| `AI_EMBEDDING_DIMENSIONS` | `1024` | Embedding dimension, aligned with Oracle `VECTOR(1024, FLOAT32)` |

### Local Secret Files

For the local defense environment, keep provider keys outside the repository:

```bash
/private/tmp/ai-ticket-secrets/siliconflow.env
/private/tmp/ai-ticket-secrets/deepseek.env
```

Load them before starting the backend:

```bash
set -a
source /private/tmp/ai-ticket-secrets/siliconflow.env
source /private/tmp/ai-ticket-secrets/deepseek.env
set +a
```

### Database and Redis

| Variable | Default | Description |
| --- | --- | --- |
| `APP_DATASOURCE_URL` | `jdbc:oracle:thin:@localhost:1521/FREEPDB1` | Oracle connection URL |
| `APP_DATASOURCE_USERNAME` | `AI_TICKET` | Oracle application user |
| `APP_DATASOURCE_PASSWORD` | `ai_ticket_pwd` | Oracle application password |
| `APP_REDIS_HOST` | `localhost` | Redis host |
| `APP_REDIS_PORT` | `6379` | Redis port |
| `APP_JWT_SECRET` | `dev-only-change-me-to-a-long-random-secret` | JWT signing secret for local development |

`docker-compose.yml` starts Oracle 23ai and Redis with defaults matching the backend configuration.

## 2. Run the Project

### Prerequisites

- Docker and Docker Compose
- Java 21
- Maven
- Node.js and npm

Use this Java 21 path on the current machine:

```bash
/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

Do not rely on `/usr/libexec/java_home -v 21`, because it may resolve to Java 17.

### Start Infrastructure

```bash
docker compose up -d
docker compose ps
```

Expected services:

- Oracle 23ai on `localhost:1521`
- Redis on `localhost:6379`

### Start Backend

```bash
cd backend

set -a
source /private/tmp/ai-ticket-secrets/siliconflow.env
source /private/tmp/ai-ticket-secrets/deepseek.env
set +a

JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn spring-boot:run
```

Backend reachability check:

```bash
curl -i http://127.0.0.1:8080/api/auth/me
```

Expected unauthenticated response:

```text
HTTP/1.1 401
```

### Start Frontend

```bash
cd frontend
npm install
npm run dev -- --host 127.0.0.1 --port 5175
```

Open:

```text
http://127.0.0.1:5175
```

### Demo Accounts

| Role | Username | Password |
| --- | --- | --- |
| Admin | `admin` | `Admin_123456` |
| User | `user` | `Admin_123456` |
| Agent | `agent` | `Admin_123456` |

Some recovered demo databases may also contain `agent2 / Admin_123456`. The assignment recommendation panel may choose `agent2` if it has the lowest active workload.

### Verification

Backend:

```bash
cd backend
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
mvn test
```

Frontend:

```bash
cd frontend
npm run test
npm run build
```

Full acceptance evidence:

```bash
FRONTEND_BASE_URL=http://127.0.0.1:5175 \
JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home \
PATH=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin:$PATH \
tools/smoke/phase31-acceptance-evidence.sh
```

Expected checks:

```text
Backend smoke                  PASS
Frontend dev smoke             PASS
Frontend tests                 PASS
Frontend build                 PASS
Backend documentation coverage PASS
```

## 3. Original Project Goal

The original goal of this graduation project was to build a practical enterprise support platform that combines:

- AI knowledge-base question answering;
- vector retrieval over enterprise documents;
- ticket handoff when AI cannot fully solve the problem;
- role-based collaboration between users, agents, and administrators;
- traceable workflow records and management statistics.

The first version focused on making the core path work end to end:

```text
Knowledge base -> RAG answer -> AI-to-ticket handoff -> admin assignment -> agent processing -> user closure -> admin statistics
```

The current implementation also includes two later extensions:

- assignment recommendation based on active agent workload;
- priority-based SLA deadline and status visibility.

Advanced features such as automatic assignment, background SLA escalation, notification delivery, multi-department approval, and production deployment remain future extensions.

## 4. Useful Documents

Only the main documents needed for review, defense, or continued development are listed here.

| Document | Purpose |
| --- | --- |
| [Final defense demo runbook](docs/demo/final-defense-runbook.md) | Step-by-step manual demo path |
| [V1 acceptance checklist](docs/acceptance/v1-acceptance-checklist.md) | Acceptance criteria and evidence mapping |
| [V1 project plan](docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-v1-project-plan.md) | Original project goals, scope, and extensibility plan |
| [Overall system design](docs/superpowers/specs/2026-06-19-ai-knowledge-ticket-system-design.md) | Architecture and domain design |
| [Assignment recommendation and SLA design](docs/superpowers/specs/2026-06-23-phase-39-40-assignment-sla-design.md) | Phase 39/40 feature design |
| [Phase 39/40 implementation plan](docs/superpowers/plans/2026-06-23-phase-39-40-assignment-sla-implementation-plan.md) | Implementation and verification checklist |
| [RAG evaluation set](docs/evaluation/rag-evaluation-set.md) | Evaluation method for retrieval and answer quality |

## 5. Repository Structure

```text
.
├── backend/                 # Spring Boot backend
├── frontend/                # Vue 3 frontend
├── docs/                    # design, demo, acceptance, and evaluation documents
├── tools/smoke/             # smoke and acceptance scripts
├── docker-compose.yml       # Oracle 23ai and Redis
└── .env.example             # local environment example
```

## 6. Security Notes

- Do not commit API keys, JWTs, `.env` files, or local secret files.
- Keep DeepSeek and SiliconFlow credentials outside the repository.
- Smoke and acceptance scripts redact tokens before writing evidence.
- Demo passwords are for local development and defense rehearsal only.
- Rotate all credentials before deploying outside a local environment.

## 7. License

This project is released under the MIT License. See [LICENSE](LICENSE).
