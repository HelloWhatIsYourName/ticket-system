<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import EmptyState from '../../components/common/EmptyState.vue'
import ErrorState from '../../components/common/ErrorState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import {
  listAssignedTickets,
  listManagedTickets,
  listMyTickets,
  type SlaStatus,
  type TicketPriority,
  type TicketStatus,
  type TicketSummary
} from '../../api/tickets'

const route = useRoute()
const tickets = ref<TicketSummary[]>([])
const loading = ref(true)
const error = ref('')
const isAssignedMode = computed(() => route.name === 'assigned-tickets')
const isManagedMode = computed(() => route.name === 'managed-tickets')
const pageTitle = computed(() => {
  if (isManagedMode.value) {
    return '工单管理'
  }

  return isAssignedMode.value ? '分配给我的工单' : '我的工单'
})
const pageDescription = computed(() =>
  isManagedMode.value
    ? '管理员可以查看待分配和处理中工单，并进入详情完成分派'
    : isAssignedMode.value
      ? '坐席需要处理的问题会在这里继续推进'
      : '从 AI 会话转入的问题会在这里继续跟踪'
)
const emptyMessage = computed(() => {
  if (isManagedMode.value) {
    return '暂无可管理工单'
  }

  return isAssignedMode.value ? '暂无分配给你的工单' : '暂无工单'
})
const tableLabel = computed(() => {
  if (isManagedMode.value) {
    return '工单管理列表'
  }

  return isAssignedMode.value ? '分配给我的工单列表' : '我的工单列表'
})

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

const slaLabel: Record<SlaStatus, string> = {
  ON_TRACK: '正常',
  DUE_SOON: '即将超时',
  OVERDUE: '已超时',
  COMPLETED: '已完成'
}

function formatStatus(status: TicketStatus) {
  return statusLabel[status] ?? status
}

function formatPriority(priority?: TicketPriority | null) {
  return priority ? priorityLabel[priority] : '未定'
}

function formatSla(status?: SlaStatus | null) {
  return status ? slaLabel[status] : '未设置'
}

function formatSource(source?: string) {
  return source === 'AI_SESSION' ? 'AI 会话' : '人工创建'
}

function formatDate(value?: string) {
  if (!value) {
    return '未记录'
  }

  return value.replace('T', ' ').slice(0, 16)
}

async function loadTickets() {
  loading.value = true
  error.value = ''
  try {
    if (isManagedMode.value) {
      tickets.value = await listManagedTickets()
    } else if (isAssignedMode.value) {
      tickets.value = await listAssignedTickets()
    } else {
      tickets.value = await listMyTickets()
    }
  } catch (err) {
    tickets.value = []
    error.value = err instanceof Error ? err.message : '工单加载失败'
  } finally {
    loading.value = false
  }
}

watch(() => route.name, loadTickets, { immediate: true })
</script>

<template>
  <section class="ticket-list-view">
    <header class="workspace-page-header">
      <div>
        <p>Ticket workspace</p>
        <h3>{{ pageTitle }}</h3>
      </div>
      <span>{{ pageDescription }}</span>
    </header>

    <LoadingState v-if="loading" message="正在加载工单" />
    <ErrorState v-else-if="error" :message="error" />
    <EmptyState v-else-if="tickets.length === 0" :message="emptyMessage" />

    <section v-else class="ticket-table" :aria-label="tableLabel">
      <div class="ticket-row ticket-row-head">
        <span>编号</span>
        <span>标题</span>
        <span>状态</span>
        <span>优先级</span>
        <span>SLA</span>
        <span>来源</span>
        <span>创建时间</span>
      </div>
      <article v-for="ticket in tickets" :key="ticket.id" class="ticket-row">
        <RouterLink class="ticket-no ticket-cell-no" :to="`/app/tickets/${ticket.id}`">{{ ticket.ticketNo }}</RouterLink>
        <span class="ticket-cell-title">
          <strong>{{ ticket.title }}</strong>
          <small>{{ ticket.transferReason || '暂无转人工原因' }}</small>
        </span>
        <span class="ticket-cell-status" data-label="状态">
          <mark class="ticket-chip">{{ formatStatus(ticket.status) }}</mark>
        </span>
        <span class="ticket-cell-priority" data-label="优先级">{{ formatPriority(ticket.priority) }}</span>
        <span class="ticket-cell-sla" data-label="SLA">
          <mark class="ticket-chip" :class="`sla-${ticket.slaStatus || 'unknown'}`">
            {{ formatSla(ticket.slaStatus) }}
          </mark>
          <small v-if="ticket.deadlineAt">{{ formatDate(ticket.deadlineAt) }}</small>
        </span>
        <span class="ticket-cell-source" data-label="来源">{{ formatSource(ticket.source) }}</span>
        <span class="ticket-cell-created" data-label="创建时间">{{ formatDate(ticket.createdAt) }}</span>
      </article>
    </section>
  </section>
</template>
