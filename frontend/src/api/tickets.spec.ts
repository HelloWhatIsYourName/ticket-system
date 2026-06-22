import { beforeEach, describe, expect, it, vi } from 'vitest'
import { http } from './http'
import {
  createTicketComment,
  createTicketFromAiSession,
  getAssignmentRecommendation,
  getTicket,
  listAssignedTickets,
  listManagedTickets,
  listMyTickets,
  listTicketComments,
  listTicketCategories,
  resolveTicket,
  startTicket
} from './tickets'

vi.mock('./http', async () => {
  const actual = await vi.importActual<typeof import('./http')>('./http')

  return {
    ...actual,
    http: {
      get: vi.fn(),
      post: vi.fn()
    }
  }
})

const getMock = vi.mocked(http.get)
const postMock = vi.mocked(http.post)

describe('tickets api', () => {
  beforeEach(() => {
    getMock.mockReset()
    postMock.mockReset()
  })

  it('loads enabled ticket categories', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        success: true,
        data: [{ id: 1, name: '账号问题', enabled: true }],
        message: 'ok'
      }
    })

    await expect(listTicketCategories()).resolves.toEqual([{ id: 1, name: '账号问题', enabled: true }])
    expect(getMock).toHaveBeenCalledWith('/ticket-categories', { params: { includeDisabled: false } })
  })

  it('creates a ticket from an AI session', async () => {
    postMock.mockResolvedValueOnce({
      data: {
        success: true,
        data: { id: 8, ticketNo: 'TK-20260620-0001', title: '无法登录', status: 'PENDING_ASSIGN' },
        message: 'ok'
      }
    })

    await expect(
      createTicketFromAiSession({
        sessionId: 7,
        assistantMessageId: 9,
        title: '无法登录',
        description: 'AI 建议转人工',
        priority: 'HIGH',
        transferReason: 'AI 置信度低'
      })
    ).resolves.toMatchObject({ id: 8, ticketNo: 'TK-20260620-0001' })
    expect(postMock).toHaveBeenCalledWith(
      '/tickets/from-ai-session',
      expect.objectContaining({ sessionId: 7, priority: 'HIGH' })
    )
  })

  it('loads my, assigned, and managed tickets', async () => {
    getMock
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: [{ id: 1, ticketNo: 'TK-1', title: '我的工单' }],
          message: 'ok'
        }
      })
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: [{ id: 2, ticketNo: 'TK-2', title: '待处理工单' }],
          message: 'ok'
        }
      })
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: [{ id: 3, ticketNo: 'TK-3', title: '全部工单' }],
          message: 'ok'
        }
      })

    await expect(listMyTickets()).resolves.toHaveLength(1)
    await expect(listAssignedTickets()).resolves.toHaveLength(1)
    await expect(listManagedTickets()).resolves.toHaveLength(1)
    expect(getMock).toHaveBeenNthCalledWith(1, '/tickets/my')
    expect(getMock).toHaveBeenNthCalledWith(2, '/tickets/assigned')
    expect(getMock).toHaveBeenNthCalledWith(3, '/tickets/manage')
  })

  it('loads ticket detail and comments', async () => {
    getMock
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 8, ticketNo: 'TK-8', title: '无法登录', status: 'PENDING_ASSIGN', flowLogs: [] },
          message: 'ok'
        }
      })
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: [{ id: 3, ticketId: 8, commentType: 'AGENT_REPLY', content: '已收到', internal: false }],
          message: 'ok'
        }
      })

    await expect(getTicket(8)).resolves.toMatchObject({ ticketNo: 'TK-8' })
    await expect(listTicketComments(8)).resolves.toHaveLength(1)
    expect(getMock).toHaveBeenNthCalledWith(1, '/tickets/8')
    expect(getMock).toHaveBeenNthCalledWith(2, '/tickets/8/comments')
  })

  it('loads assignment recommendation for a ticket', async () => {
    getMock.mockResolvedValueOnce({
      data: {
        success: true,
        data: {
          recommendedAssigneeId: 3,
          recommendedUsername: 'agent',
          recommendedDisplayName: '演示坐席',
          activeTicketCount: 2,
          reason: '推荐演示坐席：当前在办 2 单，是当前负载最低坐席'
        },
        message: 'ok'
      }
    })

    await expect(getAssignmentRecommendation(8)).resolves.toMatchObject({
      recommendedAssigneeId: 3,
      recommendedUsername: 'agent'
    })
    expect(getMock).toHaveBeenCalledWith('/tickets/8/assignment-recommendation')
  })

  it('creates comments and moves ticket workflow status', async () => {
    postMock
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 5, ticketId: 8, commentType: 'AGENT_REPLY', content: '已收到', internal: false },
          message: 'ok'
        }
      })
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 8, ticketNo: 'TK-8', status: 'PROCESSING' },
          message: 'ok'
        }
      })
      .mockResolvedValueOnce({
        data: {
          success: true,
          data: { id: 8, ticketNo: 'TK-8', status: 'RESOLVED' },
          message: 'ok'
        }
      })

    await expect(createTicketComment(8, { commentType: 'AGENT_REPLY', content: '已收到' })).resolves.toMatchObject({
      content: '已收到'
    })
    await expect(startTicket(8, '开始处理')).resolves.toMatchObject({ status: 'PROCESSING' })
    await expect(resolveTicket(8, '已解决')).resolves.toMatchObject({ status: 'RESOLVED' })
    expect(postMock).toHaveBeenNthCalledWith(1, '/tickets/8/comments', { commentType: 'AGENT_REPLY', content: '已收到' })
    expect(postMock).toHaveBeenNthCalledWith(2, '/tickets/8/start', { comment: '开始处理' })
    expect(postMock).toHaveBeenNthCalledWith(3, '/tickets/8/resolve', { comment: '已解决' })
  })
})
