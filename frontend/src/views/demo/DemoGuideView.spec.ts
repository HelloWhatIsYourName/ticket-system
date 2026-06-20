import { mount } from '@vue/test-utils'
import { describe, expect, it } from 'vitest'
import DemoGuideView from './DemoGuideView.vue'

describe('DemoGuideView', () => {
  it('renders the defense demo path with links to implemented workflows', () => {
    const wrapper = mount(DemoGuideView, {
      global: {
        stubs: {
          RouterLink: {
            props: ['to'],
            template: '<a :href="to"><slot /></a>'
          }
        }
      }
    })

    expect(wrapper.text()).toContain('答辩演示导览')
    expect(wrapper.text()).toContain('知识录入')
    expect(wrapper.text()).toContain('AI 问答')
    expect(wrapper.text()).toContain('转工单')
    expect(wrapper.text()).toContain('处理工单')
    expect(wrapper.text()).toContain('统计看板')
    expect(wrapper.find('a[href="/app/knowledge"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/app/ai/chat"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/app/tickets/my"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/app/admin/dashboard"]').exists()).toBe(true)
    expect(wrapper.find('a[href="/app/system"]').exists()).toBe(true)
  })
})
