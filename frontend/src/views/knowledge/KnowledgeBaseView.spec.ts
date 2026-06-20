import { flushPromises, mount } from '@vue/test-utils'
import { describe, expect, it, vi } from 'vitest'
import KnowledgeBaseView from './KnowledgeBaseView.vue'
import { createTextDocument, listDocuments, searchKnowledge, uploadKnowledgeDocument } from '../../api/knowledge'

vi.mock('../../api/knowledge', () => ({
  createTextDocument: vi.fn(),
  listDocuments: vi.fn(),
  searchKnowledge: vi.fn(),
  uploadKnowledgeDocument: vi.fn()
}))

const listDocumentsMock = vi.mocked(listDocuments)
const createTextDocumentMock = vi.mocked(createTextDocument)
const searchKnowledgeMock = vi.mocked(searchKnowledge)
const uploadKnowledgeDocumentMock = vi.mocked(uploadKnowledgeDocument)

describe('KnowledgeBaseView', () => {
  it('renders documents, creates text documents, and displays search results', async () => {
    listDocumentsMock
      .mockResolvedValueOnce([
        {
          id: 1,
          title: '账号手册',
          categoryId: 1,
          enabled: true,
          parseStatus: 'PARSED',
          retryCount: 0
        }
      ])
      .mockResolvedValueOnce([
        {
          id: 2,
          title: '密码手册',
          categoryId: 1,
          enabled: true,
          parseStatus: 'PARSED',
          retryCount: 0
        }
      ])
      .mockResolvedValueOnce([
        {
          id: 3,
          title: '上传政策',
          categoryId: 1,
          enabled: true,
          parseStatus: 'PARSED',
          retryCount: 0
        }
      ])
    createTextDocumentMock.mockResolvedValue({
      id: 2,
      title: '密码手册',
      categoryId: 1,
      enabled: true,
      parseStatus: 'PARSED',
      retryCount: 0
    })
    searchKnowledgeMock.mockResolvedValue([
      {
        chunkId: 8,
        documentId: 1,
        categoryId: 1,
        sourceTitle: '账号手册',
        content: '账号安全页面提供密码重置入口。',
        similarity: 0.91
      }
    ])
    uploadKnowledgeDocumentMock.mockResolvedValue({
      id: 3,
      title: '上传政策',
      categoryId: 1,
      enabled: true,
      parseStatus: 'PARSED',
      retryCount: 0
    })

    const wrapper = mount(KnowledgeBaseView)
    await flushPromises()

    expect(wrapper.text()).toContain('知识库管理')
    expect(wrapper.text()).toContain('文本录入')
    expect(wrapper.text()).toContain('检索测试')
    expect(wrapper.text()).toContain('账号手册')
    expect(wrapper.text()).toContain('PARSED')

    await wrapper.find('[data-testid="document-title"]').setValue('密码手册')
    await wrapper.find('[data-testid="document-content"]').setValue('重置密码步骤')
    await wrapper.find('[data-testid="create-document"]').trigger('click')
    await flushPromises()

    expect(createTextDocumentMock).toHaveBeenCalledWith({
      title: '密码手册',
      content: '重置密码步骤',
      categoryId: 1
    })

    await wrapper.find('[data-testid="document-title"]').setValue('上传政策')
    const fileInput = wrapper.find('[data-testid="document-file"]')
    const file = new File(['文件上传内容'], 'policy.md', { type: 'text/markdown' })
    Object.defineProperty(fileInput.element, 'files', {
      value: [file],
      configurable: true
    })
    await fileInput.trigger('change')
    await wrapper.find('[data-testid="upload-document"]').trigger('click')
    await flushPromises()

    expect(uploadKnowledgeDocumentMock).toHaveBeenCalledWith({
      file,
      title: '上传政策',
      categoryId: 1
    })

    await wrapper.find('[data-testid="search-query"]').setValue('重置密码')
    await wrapper.find('[data-testid="search-button"]').trigger('click')
    await flushPromises()

    expect(searchKnowledgeMock).toHaveBeenCalledWith({
      query: '重置密码',
      topK: 5,
      minSimilarity: 0.2
    })
    expect(wrapper.text()).toContain('账号安全页面提供密码重置入口。')
    expect(wrapper.text()).toContain('91%')
  })
})
