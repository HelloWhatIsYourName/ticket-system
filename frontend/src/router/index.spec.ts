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

  it('exposes assigned tickets inside the app shell', () => {
    const appRoute = routes.find((route) => route.path === '/app')

    expect(appRoute?.children?.map((route) => route.path)).toEqual(
      expect.arrayContaining(['tickets/assigned'])
    )
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
})
