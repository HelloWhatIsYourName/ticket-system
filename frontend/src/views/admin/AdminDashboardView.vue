<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import ErrorState from '../../components/common/ErrorState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import {
  getHotQuestions,
  getOverview,
  getTicketCategoryStats,
  type AdminDashboardOverview,
  type HotQuestionStat,
  type TicketCategoryStat
} from '../../api/adminStatistics'

const overview = ref<AdminDashboardOverview | null>(null)
const categories = ref<TicketCategoryStat[]>([])
const hotQuestions = ref<HotQuestionStat[]>([])
const loading = ref(true)
const error = ref('')

const hitRateText = computed(() => {
  const value = overview.value?.knowledgeHitRate ?? 0

  return `${Math.round(value * 100)}%`
})

onMounted(async () => {
  try {
    const [overviewResult, categoryResult, hotQuestionResult] = await Promise.all([
      getOverview(),
      getTicketCategoryStats(8),
      getHotQuestions(8)
    ])

    overview.value = overviewResult
    categories.value = categoryResult
    hotQuestions.value = hotQuestionResult
  } catch (err) {
    error.value = err instanceof Error ? err.message : '统计数据加载失败'
  } finally {
    loading.value = false
  }
})
</script>

<template>
  <section class="admin-dashboard">
    <header class="dashboard-header">
      <div>
        <p>Admin dashboard</p>
        <h3>管理统计</h3>
      </div>
    </header>

    <LoadingState v-if="loading" />
    <ErrorState v-else-if="error" :message="error" />

    <template v-else-if="overview">
      <div class="dashboard-stat-grid">
        <article class="dashboard-stat">
          <span>工单总量</span>
          <strong>{{ overview.totalTickets }}</strong>
        </article>
        <article class="dashboard-stat">
          <span>待处理</span>
          <strong>{{ overview.pendingTickets }}</strong>
        </article>
        <article class="dashboard-stat">
          <span>处理中</span>
          <strong>{{ overview.processingTickets }}</strong>
        </article>
        <article class="dashboard-stat">
          <span>已解决</span>
          <strong>{{ overview.resolvedTickets }}</strong>
        </article>
        <article class="dashboard-stat">
          <span>已关闭</span>
          <strong>{{ overview.closedTickets }}</strong>
        </article>
        <article class="dashboard-stat">
          <span>平均处理时长</span>
          <strong>{{ overview.averageResolveHours.toFixed(1) }}h</strong>
        </article>
        <article class="dashboard-stat">
          <span>知识库命中率</span>
          <strong>{{ hitRateText }}</strong>
        </article>
      </div>

      <div class="dashboard-panels">
        <section class="dashboard-panel">
          <h4>分类分布</h4>
          <EmptyState v-if="categories.length === 0" message="暂无分类统计" />
          <ul v-else class="dashboard-list">
            <li v-for="category in categories" :key="category.categoryId ?? category.categoryName">
              <span>{{ category.categoryName }}</span>
              <strong>{{ category.ticketCount }}</strong>
            </li>
          </ul>
        </section>

        <section class="dashboard-panel">
          <h4>热门问题</h4>
          <EmptyState v-if="hotQuestions.length === 0" message="暂无热门问题" />
          <ul v-else class="dashboard-list">
            <li v-for="question in hotQuestions" :key="question.question">
              <span>{{ question.question }}</span>
              <strong>{{ question.askCount }}</strong>
            </li>
          </ul>
        </section>
      </div>
    </template>
  </section>
</template>
