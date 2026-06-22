import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import AdminDashboardView from './AdminDashboardView.vue'
import { getHotQuestions, getOverview, getTicketCategoryStats } from '../../api/adminStatistics'

vi.mock('../../api/adminStatistics', () => ({
  getOverview: vi.fn(),
  getTicketCategoryStats: vi.fn(),
  getHotQuestions: vi.fn()
}))

const overviewMock = vi.mocked(getOverview)
const categoryMock = vi.mocked(getTicketCategoryStats)
const hotQuestionMock = vi.mocked(getHotQuestions)

describe('AdminDashboardView', () => {
  it('renders dashboard labels from mocked API responses', async () => {
    overviewMock.mockResolvedValue({
      totalTickets: 12,
      pendingTickets: 3,
      processingTickets: 2,
      resolvedTickets: 5,
      closedTickets: 2,
      averageResolveHours: 4.5,
      knowledgeDocuments: 8,
      aiQuestions: 24,
      knowledgeHitRate: 0.72
    })
    categoryMock.mockResolvedValue([{ categoryId: 1, categoryName: '账号问题', ticketCount: 4 }])
    hotQuestionMock.mockResolvedValue([{ question: '如何重置密码？', askCount: 7 }])

    const wrapper = mount(AdminDashboardView)
    await flushPromises()

    expect(wrapper.text()).toContain('工单总量')
    expect(wrapper.text()).toContain('待处理')
    expect(wrapper.text()).toContain('处理中')
    expect(wrapper.text()).toContain('已解决')
    expect(wrapper.text()).toContain('已关闭')
    expect(wrapper.text()).toContain('2')
    expect(wrapper.text()).toContain('5')
    expect(wrapper.text()).toContain('平均处理时长')
    expect(wrapper.text()).toContain('知识库命中率')
    expect(wrapper.text()).toContain('热门问题')
    expect(wrapper.text()).toContain('分类分布')
  })
})
