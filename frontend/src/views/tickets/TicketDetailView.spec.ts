import { flushPromises, mount } from '@vue/test-utils'
import { createPinia, setActivePinia } from 'pinia'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TicketDetailView from './TicketDetailView.vue'
import {
  assignTicket,
  confirmCloseTicket,
  createTicketComment,
  getAssignmentRecommendation,
  getTicket,
  listTicketComments,
  reopenTicket,
  resolveTicket,
  startTicket
} from '../../api/tickets'
import { listSystemRoles, listSystemUsers } from '../../api/systemAdmin'
import { useAuthStore } from '../../stores/auth'

vi.mock('vue-router', () => ({
  useRoute: () => ({
    params: {
      ticketId: '8'
    }
  })
}))

vi.mock('../../api/tickets', () => ({
  assignTicket: vi.fn(),
  closeTicket: vi.fn(),
  confirmCloseTicket: vi.fn(),
  createTicketComment: vi.fn(),
  getAssignmentRecommendation: vi.fn(),
  getTicket: vi.fn(),
  listTicketComments: vi.fn(),
  reopenTicket: vi.fn(),
  resolveTicket: vi.fn(),
  startTicket: vi.fn()
}))

vi.mock('../../api/systemAdmin', () => ({
  listSystemRoles: vi.fn(),
  listSystemUsers: vi.fn()
}))

const assignTicketMock = vi.mocked(assignTicket)
const getAssignmentRecommendationMock = vi.mocked(getAssignmentRecommendation)
const getTicketMock = vi.mocked(getTicket)
const listTicketCommentsMock = vi.mocked(listTicketComments)
const createTicketCommentMock = vi.mocked(createTicketComment)
const startTicketMock = vi.mocked(startTicket)
const resolveTicketMock = vi.mocked(resolveTicket)
const reopenTicketMock = vi.mocked(reopenTicket)
const confirmCloseTicketMock = vi.mocked(confirmCloseTicket)
const listSystemRolesMock = vi.mocked(listSystemRoles)
const listSystemUsersMock = vi.mocked(listSystemUsers)

describe('TicketDetailView', () => {
  beforeEach(() => {
    getTicketMock.mockReset()
    getAssignmentRecommendationMock.mockReset()
    listTicketCommentsMock.mockReset()
    createTicketCommentMock.mockReset()
    assignTicketMock.mockReset()
    startTicketMock.mockReset()
    resolveTicketMock.mockReset()
    reopenTicketMock.mockReset()
    confirmCloseTicketMock.mockReset()
    listSystemRolesMock.mockReset()
    listSystemUsersMock.mockReset()
  })

  it('renders ticket detail, comments, and workflow actions', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:process']
    auth.user = { id: 3, username: 'agent', displayName: 'Agent' }

    getTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      description: '用户反馈后台登录失败。',
      status: 'PENDING_PROCESS',
      priority: 'HIGH',
      source: 'AI_SESSION',
      assigneeId: 3,
      creatorId: 2,
      transferReason: 'AI 置信度低，需要人工处理',
      aiSummary: '用户无法登录后台',
      aiSuggestion: '检查账号状态和密码策略',
      createdAt: '2026-06-20T10:00:00',
      flowLogs: [
        {
          id: 1,
          ticketId: 8,
          action: 'CREATE',
          operatorId: 2,
          commentText: 'AI 会话转入工单',
          createdAt: '2026-06-20T10:00:00'
        }
      ]
    })
    listTicketCommentsMock.mockResolvedValue([
      {
        id: 3,
        ticketId: 8,
        authorId: 2,
        commentType: 'AGENT_REPLY',
        content: '已收到，正在排查。',
        internal: false,
        createdAt: '2026-06-20T10:05:00'
      },
      {
        id: 4,
        ticketId: 8,
        authorId: 3,
        commentType: 'INTERNAL_NOTE',
        content: '内部备注：疑似账号锁定。',
        internal: true,
        createdAt: '2026-06-20T10:08:00'
      }
    ])
    createTicketCommentMock.mockResolvedValue({
      id: 5,
      ticketId: 8,
      authorId: 2,
      commentType: 'AGENT_REPLY',
      content: '请用户重新尝试登录。',
      internal: false
    })
    startTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      status: 'PROCESSING'
    })
    resolveTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      status: 'RESOLVED'
    })

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('TK-20260620-0001')
    expect(wrapper.text()).toContain('无法登录后台')
    expect(wrapper.text()).toContain('用户反馈后台登录失败。')
    expect(wrapper.text()).toContain('AI 置信度低，需要人工处理')
    expect(wrapper.text()).toContain('AI 会话转入工单')
    expect(wrapper.text()).toContain('坐席回复')
    expect(wrapper.text()).toContain('已收到，正在排查。')
    expect(wrapper.text()).toContain('内部备注：疑似账号锁定。')
    expect(wrapper.text()).toContain('开始处理')
    expect(wrapper.text()).not.toContain('标记解决')

    await wrapper.find('[data-testid="action-comment"]').setValue('开始处理这个工单')
    await wrapper.find('[data-testid="start-ticket"]').trigger('click')
    await flushPromises()

    expect(startTicketMock).toHaveBeenCalledWith(8, '开始处理这个工单')

    await wrapper.find('.comment-form textarea').setValue('请用户重新尝试登录。')
    await wrapper.find('.comment-form').trigger('submit')
    await flushPromises()

    expect(createTicketCommentMock).toHaveBeenCalledWith(8, {
      commentType: 'AGENT_REPLY',
      content: '请用户重新尝试登录。'
    })
  })

  it('refreshes workflow logs after a status action so the submitted remark is visible', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:process']
    auth.user = { id: 3, username: 'agent', displayName: 'Agent' }

    getTicketMock
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'PENDING_PROCESS',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: 3,
        creatorId: 2,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: [
          {
            id: 1,
            ticketId: 8,
            action: 'ASSIGN',
            operatorId: 1,
            commentText: '分配给 agent',
            createdAt: '2026-06-20T10:01:00'
          }
        ]
      })
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'PROCESSING',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: 3,
        creatorId: 2,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: [
          {
            id: 1,
            ticketId: 8,
            action: 'ASSIGN',
            operatorId: 1,
            commentText: '分配给 agent',
            createdAt: '2026-06-20T10:01:00'
          },
          {
            id: 2,
            ticketId: 8,
            action: 'START_PROCESS',
            operatorId: 3,
            commentText: '开始处理这个工单',
            createdAt: '2026-06-20T10:05:00'
          }
        ]
      })
    listTicketCommentsMock.mockResolvedValue([])
    startTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      status: 'PROCESSING'
    })

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    await wrapper.find('[data-testid="action-comment"]').setValue('开始处理这个工单')
    await wrapper.find('[data-testid="start-ticket"]').trigger('click')
    await flushPromises()

    expect(startTicketMock).toHaveBeenCalledWith(8, '开始处理这个工单')
    expect(getTicketMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('开始处理这个工单')
  })

  it('only shows workflow actions allowed by current role and ticket status', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:process']
    auth.user = { id: 3, username: 'agent', displayName: 'Agent' }

    getTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      description: '用户反馈后台登录失败。',
      status: 'PROCESSING',
      priority: 'HIGH',
      source: 'AI_SESSION',
      assigneeId: 3,
      creatorId: 2,
      createdAt: '2026-06-20T10:00:00',
      flowLogs: []
    })
    listTicketCommentsMock.mockResolvedValue([])

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    const actionButtons = wrapper.findAll('.ticket-action-grid button').map((button) => button.text())
    expect(actionButtons).toEqual(['标记解决'])
  })

  it('lets an admin assign a pending ticket to an active agent from the ticket detail page', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:manage', 'ticket:assign', 'system:user:manage', 'system:role:manage']
    auth.user = { id: 2, username: 'admin', displayName: 'Admin' }

    getTicketMock
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'PENDING_ASSIGN',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: null,
        creatorId: 4,
        deadlineAt: '2026-06-20T18:00:00',
        slaStatus: 'DUE_SOON',
        slaRemainingMinutes: 90,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: []
      })
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'PENDING_PROCESS',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: 3,
        creatorId: 4,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: [
          {
            id: 9,
            ticketId: 8,
            action: 'ASSIGN',
            operatorId: 2,
            commentText: '分配给 agent',
            createdAt: '2026-06-20T10:03:00'
          }
        ]
      })
    listTicketCommentsMock.mockResolvedValue([])
    listSystemRolesMock.mockResolvedValue([
      { id: 3, roleCode: 'AGENT', roleName: '坐席或工程师', status: 'ACTIVE' },
      { id: 4, roleCode: 'USER', roleName: '普通用户', status: 'ACTIVE' }
    ])
    listSystemUsersMock.mockResolvedValue([
      { id: 3, username: 'agent', displayName: '演示坐席', status: 'ACTIVE', roleIds: [3] },
      { id: 4, username: 'user', displayName: '普通用户', status: 'ACTIVE', roleIds: [4] },
      { id: 5, username: 'disabled-agent', displayName: '禁用坐席', status: 'DISABLED', roleIds: [3] }
    ])
    getAssignmentRecommendationMock.mockResolvedValue({
      recommendedAssigneeId: 3,
      recommendedUsername: 'agent',
      recommendedDisplayName: '演示坐席',
      activeTicketCount: 0,
      reason: '推荐演示坐席：当前无在办工单，可优先承接'
    })
    assignTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      status: 'PENDING_PROCESS',
      assigneeId: 3
    })

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    expect(wrapper.text()).toContain('分配工单')
    expect(wrapper.text()).toContain('智能推荐')
    expect(wrapper.text()).toContain('推荐演示坐席：当前无在办工单，可优先承接')
    expect(wrapper.text()).toContain('SLA 状态')
    expect(wrapper.text()).toContain('即将超时')
    expect(wrapper.text()).toContain('2026-06-20 18:00')
    const assigneeOptions = wrapper.find('[data-testid="assignee-select"]').findAll('option')
    expect(assigneeOptions.map((option) => option.text())).toEqual(['请选择坐席', '演示坐席（agent）'])

    await wrapper.find('[data-testid="use-recommended-assignee"]').trigger('click')
    expect((wrapper.find('[data-testid="assignee-select"]').element as HTMLSelectElement).value).toBe('3')

    await wrapper.find('[data-testid="assignee-select"]').setValue('3')
    await wrapper.find('[data-testid="action-comment"]').setValue('分配给 agent')
    await wrapper.find('[data-testid="assign-ticket"]').trigger('click')
    await flushPromises()

    expect(assignTicketMock).toHaveBeenCalledWith(8, { assigneeId: 3, comment: '分配给 agent' })
    expect(getTicketMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('分配给 agent')
  })

  it('lets the ticket creator confirm close a resolved ticket and shows the submitted workflow remark', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:view:own']
    auth.user = { id: 4, username: 'user', displayName: 'User' }

    getTicketMock
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'RESOLVED',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: 3,
        creatorId: 4,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: [
          {
            id: 1,
            ticketId: 8,
            action: 'RESOLVE',
            operatorId: 3,
            commentText: '已完成排查并给出解决方案',
            createdAt: '2026-06-20T10:12:00'
          }
        ]
      })
      .mockResolvedValueOnce({
        id: 8,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        description: '用户反馈后台登录失败。',
        status: 'CLOSED',
        priority: 'HIGH',
        source: 'AI_SESSION',
        assigneeId: 3,
        creatorId: 4,
        createdAt: '2026-06-20T10:00:00',
        flowLogs: [
          {
            id: 1,
            ticketId: 8,
            action: 'RESOLVE',
            operatorId: 3,
            commentText: '已完成排查并给出解决方案',
            createdAt: '2026-06-20T10:12:00'
          },
          {
            id: 2,
            ticketId: 8,
            action: 'CONFIRM_CLOSE',
            operatorId: 4,
            commentText: '确认问题已解决',
            createdAt: '2026-06-20T10:20:00'
          }
        ]
      })
    listTicketCommentsMock.mockResolvedValue([])
    confirmCloseTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      status: 'CLOSED'
    })

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    const actionButtons = wrapper.findAll('.ticket-action-grid button').map((button) => button.text())
    expect(actionButtons).toEqual(['重新打开', '确认关闭'])

    await wrapper.find('[data-testid="action-comment"]').setValue('确认问题已解决')
    await wrapper.find('[data-testid="confirm-close-ticket"]').trigger('click')
    await flushPromises()

    expect(confirmCloseTicketMock).toHaveBeenCalledWith(8, '确认问题已解决')
    expect(reopenTicketMock).not.toHaveBeenCalled()
    expect(getTicketMock).toHaveBeenCalledTimes(2)
    expect(wrapper.text()).toContain('确认问题已解决')
    expect(wrapper.text()).toContain('已关闭')
  })

  it('exposes responsive panel hooks for detail page layout', async () => {
    const pinia = createPinia()
    setActivePinia(pinia)
    const auth = useAuthStore()
    auth.permissions = ['ticket:process']
    auth.user = { id: 3, username: 'agent', displayName: 'Agent' }

    getTicketMock.mockResolvedValue({
      id: 8,
      ticketNo: 'TK-20260620-0001',
      title: '无法登录后台',
      description: '用户反馈后台登录失败。',
      status: 'PENDING_PROCESS',
      priority: 'HIGH',
      source: 'AI_SESSION',
      assigneeId: 3,
      creatorId: 2,
      transferReason: 'AI 置信度低，需要人工处理',
      aiSummary: '用户无法登录后台',
      aiSuggestion: '检查账号状态和密码策略',
      deadlineAt: '2026-06-20T18:00:00',
      slaStatus: 'DUE_SOON',
      slaRemainingMinutes: 90,
      createdAt: '2026-06-20T10:00:00',
      flowLogs: [
        {
          id: 1,
          ticketId: 8,
          action: 'CREATE',
          operatorId: 2,
          commentText: 'AI 会话转入工单',
          createdAt: '2026-06-20T10:00:00'
        }
      ]
    })
    listTicketCommentsMock.mockResolvedValue([
      {
        id: 3,
        ticketId: 8,
        authorId: 2,
        commentType: 'AGENT_REPLY',
        content: '已收到，正在排查。',
        internal: false,
        createdAt: '2026-06-20T10:05:00'
      }
    ])

    const wrapper = mount(TicketDetailView)
    await flushPromises()

    expect(wrapper.find('.ticket-hero-panel').exists()).toBe(true)
    expect(wrapper.find('.ticket-context-panel').exists()).toBe(true)
    expect(wrapper.find('.ticket-comments-panel').exists()).toBe(true)
    expect(wrapper.find('.ticket-sla-panel').exists()).toBe(true)
    expect(wrapper.find('.ticket-action-panel').exists()).toBe(true)
    expect(wrapper.find('.ticket-flow-panel').exists()).toBe(true)
  })
})
