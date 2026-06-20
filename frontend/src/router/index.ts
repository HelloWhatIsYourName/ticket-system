import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import AppShell from '../layouts/AppShell.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import { useAuthStore } from '../stores/auth'

interface AuthGuardStore {
  isAuthenticated: boolean
  user: unknown
  loadCurrentUser: () => Promise<void>
}

export const routes: RouteRecordRaw[] = [
  {
    path: '/',
    name: 'home',
    component: HomeView
  },
  {
    path: '/login',
    name: 'login',
    component: LoginView
  },
  {
    path: '/app',
    name: 'app',
    component: AppShell,
    meta: { requiresAuth: true },
    children: [
      {
        path: '',
        name: 'app-home',
        component: () => import('../views/demo/DemoGuideView.vue')
      },
      {
        path: 'demo',
        name: 'demo-guide',
        component: () => import('../views/demo/DemoGuideView.vue')
      },
      {
        path: 'admin/dashboard',
        name: 'admin-dashboard',
        component: () => import('../views/admin/AdminDashboardView.vue')
      },
      {
        path: 'system',
        name: 'system-admin',
        component: () => import('../views/system/SystemAdminView.vue')
      },
      {
        path: 'ai/chat',
        name: 'rag-chat',
        component: () => import('../views/ai/RagChatView.vue')
      },
      {
        path: 'knowledge',
        name: 'knowledge-base',
        component: () => import('../views/knowledge/KnowledgeBaseView.vue')
      },
      {
        path: 'tickets/my',
        name: 'my-tickets',
        component: () => import('../views/tickets/TicketListView.vue')
      },
      {
        path: 'tickets/:ticketId',
        name: 'ticket-detail',
        component: () => import('../views/tickets/TicketDetailView.vue')
      },
      {
        path: ':pathMatch(.*)*',
        name: 'app-placeholder',
        component: PlaceholderView
      }
    ]
  }
]

export async function resolveAuthNavigation(
  to: Pick<RouteLocationNormalized, 'fullPath' | 'matched'>,
  auth: AuthGuardStore = useAuthStore()
) {
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)

  if (!requiresAuth) {
    return true
  }

  if (!auth.isAuthenticated) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (!auth.user) {
    try {
      await auth.loadCurrentUser()
    } catch {
      return {
        path: '/login',
        query: { redirect: to.fullPath }
      }
    }
  }

  return true
}

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to) => resolveAuthNavigation(to))

export default router
