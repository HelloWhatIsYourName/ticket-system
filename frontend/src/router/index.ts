import { createRouter, createWebHistory, type RouteLocationNormalized, type RouteRecordRaw } from 'vue-router'
import AppShell from '../layouts/AppShell.vue'
import LoginView from '../views/LoginView.vue'
import HomeView from '../views/HomeView.vue'
import PlaceholderView from '../views/PlaceholderView.vue'
import { useAuthStore } from '../stores/auth'

interface AuthGuardStore {
  isAuthenticated: boolean
  user: unknown
  permissions?: string[]
  firstMenuPath?: string
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
        component: () => import('../views/admin/AdminDashboardView.vue'),
        meta: { requiredPermissions: ['dashboard:view'] }
      },
      {
        path: 'system',
        name: 'system-admin',
        component: () => import('../views/system/SystemAdminView.vue'),
        meta: { requiredPermissions: ['system:user:manage'] }
      },
      {
        path: 'ai/chat',
        name: 'rag-chat',
        component: () => import('../views/ai/RagChatView.vue'),
        meta: { requiredPermissions: ['ai:chat:ask'] }
      },
      {
        path: 'knowledge',
        name: 'knowledge-base',
        component: () => import('../views/knowledge/KnowledgeBaseView.vue'),
        meta: { requiredPermissions: ['knowledge:document:manage'] }
      },
      {
        path: 'tickets/my',
        name: 'my-tickets',
        component: () => import('../views/tickets/TicketListView.vue'),
        meta: { requiredPermissions: ['ticket:view:own'] }
      },
      {
        path: 'tickets/assigned',
        name: 'assigned-tickets',
        component: () => import('../views/tickets/TicketListView.vue'),
        meta: { requiredPermissions: ['ticket:process'] }
      },
      {
        path: 'tickets/manage',
        name: 'managed-tickets',
        component: () => import('../views/tickets/TicketListView.vue'),
        meta: { requiredPermissions: ['ticket:manage'] }
      },
      {
        path: 'tickets/:ticketId',
        name: 'ticket-detail',
        component: () => import('../views/tickets/TicketDetailView.vue'),
        meta: { requiredPermissions: ['ticket:view:own', 'ticket:process', 'ticket:manage'], permissionMode: 'any' }
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

  const requiredPermissions = to.matched.flatMap((record) => {
    const value = record.meta.requiredPermissions
    return Array.isArray(value) ? value : []
  })

  if (requiredPermissions.length > 0) {
    const permissions = auth.permissions ?? []
    const requiresAnyPermission = to.matched.some((record) => record.meta.permissionMode === 'any')
    const allowed = requiresAnyPermission
      ? requiredPermissions.some((permission) => permissions.includes(permission))
      : requiredPermissions.every((permission) => permissions.includes(permission))

    if (!allowed) {
      return auth.firstMenuPath ?? '/app'
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
