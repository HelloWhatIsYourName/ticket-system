<script setup lang="ts">
import { computed, onMounted, ref, watchEffect } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '../../components/common/EmptyState.vue'
import ErrorState from '../../components/common/ErrorState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import { useAuthStore } from '../../stores/auth'
import { listSystemRoles, listSystemUsers, type SystemUser } from '../../api/systemAdmin'
import {
  assignTicket,
  closeTicket,
  confirmCloseTicket,
  createTicketComment,
  getTicket,
  listTicketComments,
  reopenTicket,
  resolveTicket,
  startTicket,
  type TicketComment,
  type TicketCommentType,
  type TicketDetail,
  type TicketPriority,
  type TicketStatus,
  type TicketSummary
} from '../../api/tickets'

const route = useRoute()
const auth = useAuthStore()
const ticketId = computed(() => Number(route.params.ticketId))
const ticket = ref<TicketDetail | null>(null)
const comments = ref<TicketComment[]>([])
const loading = ref(true)
const error = ref('')
const actionComment = ref('')
const commentType = ref<TicketCommentType>('USER_REPLY')
const commentContent = ref('')
const submittingComment = ref(false)
const runningAction = ref('')
const assignees = ref<SystemUser[]>([])
const selectedAssigneeId = ref('')
const loadingAssignees = ref(false)
const canWriteAgentComment = computed(
  () => auth.permissions.includes('ticket:process') || auth.permissions.includes('ticket:manage')
)
const currentUserId = computed(() => auth.user?.id)
const isAssignedProcessor = computed(
  () => auth.permissions.includes('ticket:process') && ticket.value?.assigneeId === currentUserId.value
)
const isCreator = computed(
  () => auth.permissions.includes('ticket:view:own') && ticket.value?.creatorId === currentUserId.value
)
const canManageTicket = computed(() => auth.permissions.includes('ticket:manage'))
const canStartTicket = computed(() => isAssignedProcessor.value && ticket.value?.status === 'PENDING_PROCESS')
const canResolveTicket = computed(() => isAssignedProcessor.value && ticket.value?.status === 'PROCESSING')
const canReopenTicket = computed(() => isCreator.value && ticket.value?.status === 'RESOLVED')
const canConfirmCloseTicket = computed(() => isCreator.value && ticket.value?.status === 'RESOLVED')
const canCloseTicket = computed(() => canManageTicket.value && ticket.value?.status !== 'CLOSED')
const canAssignTicket = computed(
  () => auth.permissions.includes('ticket:assign') && canManageTicket.value && ticket.value?.status === 'PENDING_ASSIGN'
)
const hasWorkflowAction = computed(
  () =>
    canAssignTicket.value ||
    canStartTicket.value ||
    canResolveTicket.value ||
    canReopenTicket.value ||
    canConfirmCloseTicket.value ||
    canCloseTicket.value
)
const commentTypeOptions = computed(() =>
  canWriteAgentComment.value
    ? [
        { value: 'AGENT_REPLY' as TicketCommentType, label: '坐席回复' },
        { value: 'INTERNAL_NOTE' as TicketCommentType, label: '内部备注' }
      ]
    : [{ value: 'USER_REPLY' as TicketCommentType, label: '用户回复' }]
)

const statusLabel: Record<TicketStatus, string> = {
  PENDING_ASSIGN: '待分配',
  PENDING_PROCESS: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已解决',
  CLOSED: '已关闭'
}

const priorityLabel: Record<TicketPriority, string> = {
  LOW: '低',
  NORMAL: '普通',
  HIGH: '高',
  URGENT: '紧急'
}

function formatStatus(status?: TicketStatus) {
  return status ? statusLabel[status] : '未知'
}

function formatPriority(priority?: TicketPriority | null) {
  return priority ? priorityLabel[priority] : '未定'
}

function formatDate(value?: string | null) {
  if (!value) {
    return '未记录'
  }

  return value.replace('T', ' ').slice(0, 16)
}

function formatCommentType(type: TicketCommentType) {
  if (type === 'USER_REPLY') {
    return '用户回复'
  }

  if (type === 'AGENT_REPLY') {
    return '坐席回复'
  }

  if (type === 'INTERNAL_NOTE') {
    return '内部备注'
  }

  if (type === 'SYSTEM') {
    return '系统记录'
  }

  return '回复'
}

function formatFlowLogComment(log: TicketDetail['flowLogs'][number]) {
  return log.commentText || log.remark || '无备注'
}

watchEffect(() => {
  if (!commentTypeOptions.value.some((option) => option.value === commentType.value)) {
    commentType.value = commentTypeOptions.value[0].value
  }
})

async function loadTicket() {
  loading.value = true
  error.value = ''

  try {
    const [ticketResult, commentResult] = await Promise.all([
      getTicket(ticketId.value),
      listTicketComments(ticketId.value)
    ])

    ticket.value = ticketResult
    comments.value = commentResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '工单详情加载失败'
  } finally {
    loading.value = false
  }
}

async function loadAssignees() {
  if (!canManageTicket.value || !auth.permissions.includes('system:user:manage')) {
    return
  }

  loadingAssignees.value = true

  try {
    const [userResult, roleResult] = await Promise.all([listSystemUsers(100), listSystemRoles()])
    const agentRoleIds = roleResult.filter((role) => role.roleCode === 'AGENT').map((role) => role.id)

    assignees.value = userResult.filter(
      (user) => user.status === 'ACTIVE' && user.roleIds.some((roleId) => agentRoleIds.includes(roleId))
    )
  } finally {
    loadingAssignees.value = false
  }
}

async function submitComment() {
  const content = commentContent.value.trim()

  if (!content || submittingComment.value) {
    return
  }

  submittingComment.value = true

  try {
    const created = await createTicketComment(ticketId.value, {
      commentType: commentType.value,
      content
    })

    comments.value = [...comments.value, created]
    commentContent.value = ''
  } finally {
    submittingComment.value = false
  }
}

async function runAction(
  action: 'start' | 'resolve' | 'reopen' | 'confirm-close' | 'close',
  handler: (id: number, comment?: string) => Promise<TicketSummary>
) {
  if (runningAction.value) {
    return
  }

  runningAction.value = action

  try {
    const updated = await handler(ticketId.value, actionComment.value.trim() || undefined)

    if (ticket.value) {
      ticket.value = { ...ticket.value, ...updated }
    }
    await loadTicket()
    actionComment.value = ''
  } finally {
    runningAction.value = ''
  }
}

async function assignSelectedTicket() {
  const assigneeId = Number(selectedAssigneeId.value)

  if (!assigneeId || runningAction.value) {
    return
  }

  runningAction.value = 'assign'

  try {
    const updated = await assignTicket(ticketId.value, {
      assigneeId,
      comment: actionComment.value.trim() || undefined
    })

    if (ticket.value) {
      ticket.value = { ...ticket.value, ...updated }
    }
    await loadTicket()
    selectedAssigneeId.value = ''
    actionComment.value = ''
  } finally {
    runningAction.value = ''
  }
}

onMounted(async () => {
  await loadTicket()
  await loadAssignees()
})
</script>

<template>
  <section class="ticket-detail-view">
    <header class="workspace-page-header">
      <div>
        <p>Ticket detail</p>
        <h3>{{ ticket?.ticketNo || '工单详情' }}</h3>
      </div>
      <span>回复、内部备注与状态流转</span>
    </header>

    <LoadingState v-if="loading" message="正在加载工单详情" />
    <ErrorState v-else-if="error" :message="error" />
    <EmptyState v-else-if="!ticket" message="未找到工单" />

    <div v-else class="ticket-detail-layout">
      <main class="ticket-detail-main">
        <section class="ticket-detail-panel ticket-hero-panel">
          <div class="ticket-detail-meta">
            <span>{{ formatStatus(ticket.status) }}</span>
            <span>{{ formatPriority(ticket.priority) }}</span>
            <span>{{ ticket.source === 'AI_SESSION' ? 'AI 会话' : '人工创建' }}</span>
            <span>{{ formatDate(ticket.createdAt) }}</span>
          </div>
          <h3>{{ ticket.title }}</h3>
          <p>{{ ticket.description }}</p>
        </section>

        <section class="ticket-detail-panel" v-if="ticket.aiSummary || ticket.aiSuggestion || ticket.transferReason">
          <div class="panel-heading">
            <span>AI 转人工上下文</span>
          </div>
          <dl class="ticket-ai-context">
            <div v-if="ticket.aiSummary">
              <dt>AI 摘要</dt>
              <dd>{{ ticket.aiSummary }}</dd>
            </div>
            <div v-if="ticket.aiSuggestion">
              <dt>AI 建议</dt>
              <dd>{{ ticket.aiSuggestion }}</dd>
            </div>
            <div v-if="ticket.transferReason">
              <dt>转人工原因</dt>
              <dd>{{ ticket.transferReason }}</dd>
            </div>
          </dl>
        </section>

        <section class="ticket-detail-panel">
          <div class="panel-heading">
            <span>沟通记录</span>
            <strong>{{ comments.length }}</strong>
          </div>
          <EmptyState v-if="comments.length === 0" message="暂无沟通记录" />
          <article v-for="comment in comments" v-else :key="comment.id" class="comment-item">
            <div>
              <mark :class="{ 'is-internal': comment.internal }">{{ formatCommentType(comment.commentType) }}</mark>
              <time>{{ formatDate(comment.createdAt) }}</time>
            </div>
            <p>{{ comment.content }}</p>
          </article>
          <form class="comment-form" @submit.prevent="submitComment">
            <label>
              类型
              <select v-model="commentType">
                <option v-for="option in commentTypeOptions" :key="option.value" :value="option.value">
                  {{ option.label }}
                </option>
              </select>
            </label>
            <label>
              内容
              <textarea v-model="commentContent" rows="4" placeholder="输入回复或内部备注" />
            </label>
            <button type="submit" :disabled="submittingComment || !commentContent.trim()">
              {{ submittingComment ? '提交中...' : '提交记录' }}
            </button>
          </form>
        </section>
      </main>

      <aside class="ticket-detail-side">
        <section class="ticket-detail-panel">
          <div class="panel-heading">
            <span>状态动作</span>
          </div>
          <label class="action-comment-field">
            处理备注
            <textarea
              v-model="actionComment"
              data-testid="action-comment"
              rows="5"
              placeholder="给本次状态变更留一条备注"
            />
          </label>
          <div v-if="canAssignTicket" class="ticket-assignment-form">
            <strong>分配工单</strong>
            <label>
              坐席
              <select v-model="selectedAssigneeId" data-testid="assignee-select" :disabled="loadingAssignees">
                <option value="">请选择坐席</option>
                <option v-for="assignee in assignees" :key="assignee.id" :value="String(assignee.id)">
                  {{ assignee.displayName }}（{{ assignee.username }}）
                </option>
              </select>
            </label>
            <button
              data-testid="assign-ticket"
              type="button"
              :disabled="Boolean(runningAction) || !selectedAssigneeId"
              @click="assignSelectedTicket"
            >
              {{ runningAction === 'assign' ? '分配中...' : '分配给坐席' }}
            </button>
          </div>
          <div v-if="hasWorkflowAction" class="ticket-action-grid">
            <button
              v-if="canStartTicket"
              data-testid="start-ticket"
              type="button"
              :disabled="Boolean(runningAction)"
              @click="runAction('start', startTicket)"
            >
              开始处理
            </button>
            <button
              v-if="canResolveTicket"
              data-testid="resolve-ticket"
              type="button"
              :disabled="Boolean(runningAction)"
              @click="runAction('resolve', resolveTicket)"
            >
              标记解决
            </button>
            <button
              v-if="canReopenTicket"
              data-testid="reopen-ticket"
              type="button"
              :disabled="Boolean(runningAction)"
              @click="runAction('reopen', reopenTicket)"
            >
              重新打开
            </button>
            <button
              v-if="canConfirmCloseTicket"
              data-testid="confirm-close-ticket"
              type="button"
              :disabled="Boolean(runningAction)"
              @click="runAction('confirm-close', confirmCloseTicket)"
            >
              确认关闭
            </button>
            <button
              v-if="canCloseTicket"
              data-testid="close-ticket"
              type="button"
              :disabled="Boolean(runningAction)"
              @click="runAction('close', closeTicket)"
            >
              管理关闭
            </button>
          </div>
          <EmptyState v-else message="当前状态暂无可执行动作" />
        </section>

        <section class="ticket-detail-panel">
          <div class="panel-heading">
            <span>流程记录</span>
            <strong>{{ ticket.flowLogs.length }}</strong>
          </div>
          <EmptyState v-if="ticket.flowLogs.length === 0" message="暂无流程记录" />
          <article v-for="log in ticket.flowLogs" v-else :key="log.id" class="flow-log-item">
            <strong>{{ log.action }}</strong>
            <span>{{ formatFlowLogComment(log) }}</span>
            <time>{{ formatDate(log.createdAt) }}</time>
          </article>
        </section>
      </aside>
    </div>
  </section>
</template>
