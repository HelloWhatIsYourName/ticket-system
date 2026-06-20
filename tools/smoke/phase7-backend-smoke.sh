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
