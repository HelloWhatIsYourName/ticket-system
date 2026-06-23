<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const router = useRouter()

const displayName = computed(() => auth.user?.displayName || auth.user?.username || '未登录用户')
const avatarInitial = computed(() => displayName.value.slice(0, 1).toUpperCase())
const fallbackMenus = [
  { code: 'demo-guide', name: '演示导览', path: '/app/demo' },
  { code: 'ai-chat', name: 'AI 问答', path: '/app/ai/chat' },
  { code: 'knowledge-base', name: '知识库', path: '/app/knowledge' },
  { code: 'my-tickets', name: '我的工单', path: '/app/tickets/my' },
  { code: 'assigned-tickets', name: '分配给我', path: '/app/tickets/assigned' },
  { code: 'admin-dashboard', name: '管理概览', path: '/app/admin/dashboard' },
  { code: 'system-admin', name: '系统管理', path: '/app/system' }
]
const menus = computed(() => (auth.menus.length > 0 ? auth.menus : fallbackMenus))

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<template>
  <div class="app-shell">
    <aside class="app-sidebar" aria-label="Application navigation">
      <div class="app-sidebar-head">
        <RouterLink class="app-shell-brand" to="/">AI Knowledge Ticket</RouterLink>
        <span>Service Desk</span>
      </div>
      <nav class="app-shell-menu">
        <RouterLink v-for="menu in menus" :key="menu.code || menu.path" :to="menu.path">
          {{ menu.name }}
        </RouterLink>
      </nav>
    </aside>

    <section class="app-workspace">
      <header class="app-topbar">
        <div>
          <p class="app-topbar-label">Workspace</p>
          <h2>服务工作台</h2>
        </div>
        <div class="app-user">
          <span class="app-user-avatar">{{ avatarInitial }}</span>
          <span>{{ displayName }}</span>
          <button type="button" @click="logout">退出</button>
        </div>
      </header>

      <main class="app-content">
        <RouterView />
      </main>
    </section>
  </div>
</template>
