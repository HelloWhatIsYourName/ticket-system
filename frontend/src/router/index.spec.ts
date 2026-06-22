import { describe, expect, it, vi } from 'vitest'
import { resolveAuthNavigation, routes } from './index'

describe('routes', () => {
  it('exposes public, login, and app routes', () => {
    expect(routes.map((route) => route.path)).toEqual(expect.arrayContaining(['/', '/login', '/app']))
  })

  it('marks app routes as authenticated', () => {
    const appRoute = routes.find((route) => route.path === '/app')

    expect(appRoute?.meta?.requiresAuth).toBe(true)
  })

  it('exposes ticket list routes before ticket detail inside the app shell', () => {
    const appRoute = routes.find((route) => route.path === '/app')
    const paths = appRoute?.children?.map((route) => route.path) ?? []

    expect(paths).toEqual(expect.arrayContaining(['tickets/my', 'tickets/assigned', 'tickets/manage']))
    expect(paths.indexOf('tickets/manage')).toBeLessThan(paths.indexOf('tickets/:ticketId'))
  })

  it('redirects anonymous app navigation to login with redirect query', async () => {
    await expect(
      resolveAuthNavigation(
        {
          fullPath: '/app/ai/chat',
          matched: [{ meta: { requiresAuth: true } }]
        },
        {
          isAuthenticated: false,
          user: null,
          loadCurrentUser: vi.fn()
        }
      )
    ).resolves.toEqual({
      path: '/login',
      query: { redirect: '/app/ai/chat' }
    })
  })

  it('hydrates authenticated users before allowing app navigation', async () => {
    const loadCurrentUser = vi.fn().mockResolvedValue(undefined)

    await expect(
      resolveAuthNavigation(
        {
          fullPath: '/app/tickets/my',
          matched: [{ meta: { requiresAuth: true } }]
        },
        {
          isAuthenticated: true,
          user: null,
          loadCurrentUser
        }
      )
    ).resolves.toBe(true)
    expect(loadCurrentUser).toHaveBeenCalledOnce()
  })

  it('redirects authenticated users away from routes they do not have permission to access', async () => {
    await expect(
      resolveAuthNavigation(
        {
          fullPath: '/app/admin/dashboard',
          matched: [
            { meta: { requiresAuth: true } },
            { meta: { requiredPermissions: ['dashboard:view'] } }
          ]
        },
        {
          isAuthenticated: true,
          user: { id: 4, username: 'user' },
          permissions: ['ai:chat:ask', 'ticket:view:own'],
          firstMenuPath: '/app/ai/chat',
          loadCurrentUser: vi.fn()
        }
      )
    ).resolves.toEqual('/app/ai/chat')
  })
})
