import { flushPromises, mount } from '@vue/test-utils'
import { beforeEach, describe, expect, it, vi } from 'vitest'
import TicketListView from './TicketListView.vue'
import { listAssignedTickets, listManagedTickets, listMyTickets } from '../../api/tickets'

const routeState = vi.hoisted(() => ({
  name: 'my-tickets'
}))

vi.mock('../../api/tickets', () => ({
  listMyTickets: vi.fn(),
  listAssignedTickets: vi.fn(),
  listManagedTickets: vi.fn()
}))

vi.mock('vue-router', () => ({
  useRoute: () => ({ name: routeState.name })
}))

const listMyTicketsMock = vi.mocked(listMyTickets)
const listAssignedTicketsMock = vi.mocked(listAssignedTickets)
const listManagedTicketsMock = vi.mocked(listManagedTickets)

describe('TicketListView', () => {
  beforeEach(() => {
    routeState.name = 'my-tickets'
    listMyTicketsMock.mockReset()
    listAssignedTicketsMock.mockReset()
    listManagedTicketsMock.mockReset()
  })

  it('renders my tickets from the API', async () => {
    listMyTicketsMock.mockResolvedValue([
      {
        id: 1,
        ticketNo: 'TK-20260620-0001',
        title: '无法登录后台',
        status: 'PENDING_ASSIGN',
        priority: 'HIGH',
        source: 'AI_SESSION',
        transferReason: 'AI 置信度低，需要人工处理',
        createdAt: '2026-06-20T10:00:00'
      }
    ])

    const wrapper = mount(TicketListView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('我的工单')
    expect(wrapper.text()).toContain('TK-20260620-0001')
    expect(wrapper.text()).toContain('无法登录后台')
    expect(wrapper.text()).toContain('待分配')
    expect(wrapper.text()).toContain('高')
    expect(wrapper.text()).toContain('AI 置信度低，需要人工处理')
    expect(wrapper.find('a[href="/app/tickets/1"]').exists()).toBe(true)
    expect(listAssignedTicketsMock).not.toHaveBeenCalled()
    expect(listManagedTicketsMock).not.toHaveBeenCalled()
  })

  it('renders assigned tickets from the assigned API when opened as agent workspace', async () => {
    routeState.name = 'assigned-tickets'
    listAssignedTicketsMock.mockResolvedValue([
      {
        id: 2,
        ticketNo: 'TK-20260620-0002',
        title: 'VPN 连接失败',
        status: 'PENDING_PROCESS',
        priority: 'NORMAL',
        source: 'MANUAL',
        transferReason: null,
        createdAt: '2026-06-20T11:30:00'
      }
    ])

    const wrapper = mount(TicketListView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('分配给我的工单')
    expect(wrapper.text()).toContain('TK-20260620-0002')
    expect(wrapper.text()).toContain('VPN 连接失败')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('普通')
    expect(listAssignedTicketsMock).toHaveBeenCalledOnce()
    expect(listMyTicketsMock).not.toHaveBeenCalled()
    expect(listManagedTicketsMock).not.toHaveBeenCalled()
  })

  it('renders managed tickets from the management API when opened as admin workspace', async () => {
    routeState.name = 'managed-tickets'
    listManagedTicketsMock.mockResolvedValue([
      {
        id: 3,
        ticketNo: 'TK-20260620-0003',
        title: '邮箱无法收信',
        status: 'PENDING_ASSIGN',
        priority: 'URGENT',
        source: 'MANUAL',
        transferReason: null,
        createdAt: '2026-06-20T12:00:00'
      }
    ])

    const wrapper = mount(TicketListView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })
    await flushPromises()

    expect(wrapper.text()).toContain('工单管理')
    expect(wrapper.text()).toContain('TK-20260620-0003')
    expect(wrapper.text()).toContain('邮箱无法收信')
    expect(wrapper.text()).toContain('待分配')
    expect(wrapper.text()).toContain('紧急')
    expect(listManagedTicketsMock).toHaveBeenCalledOnce()
    expect(listMyTicketsMock).not.toHaveBeenCalled()
    expect(listAssignedTicketsMock).not.toHaveBeenCalled()
  })
})
