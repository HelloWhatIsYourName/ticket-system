<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import EmptyState from '../../components/common/EmptyState.vue'
import ErrorState from '../../components/common/ErrorState.vue'
import LoadingState from '../../components/common/LoadingState.vue'
import {
  createTextDocument,
  listDocuments,
  searchKnowledge,
  uploadKnowledgeDocument,
  type KnowledgeDocument,
  type KnowledgeSearchResult
} from '../../api/knowledge'

const documents = ref<KnowledgeDocument[]>([])
const results = ref<KnowledgeSearchResult[]>([])
const loading = ref(true)
const error = ref('')
const creating = ref(false)
const uploading = ref(false)
const searching = ref(false)
const title = ref('')
const categoryId = ref(1)
const content = ref('')
const selectedFile = ref<File | null>(null)
const uploadMessage = ref('')
const query = ref('')
const topK = ref(5)
const minSimilarity = ref(0.2)

const documentCountText = computed(() => `${documents.value.length} 篇`)

function similarityText(value?: number) {
  if (value === undefined || value === null) {
    return '未知'
  }

  return `${Math.round(value * 100)}%`
}

async function refreshDocuments() {
  documents.value = await listDocuments()
}

async function loadDocuments() {
  loading.value = true
  error.value = ''

  try {
    await refreshDocuments()
  } catch (err) {
    error.value = err instanceof Error ? err.message : '知识文档加载失败'
  } finally {
    loading.value = false
  }
}

async function submitDocument() {
  const normalizedTitle = title.value.trim()
  const normalizedContent = content.value.trim()

  if (!normalizedTitle || !normalizedContent || creating.value) {
    return
  }

  creating.value = true

  try {
    await createTextDocument({
      title: normalizedTitle,
      content: normalizedContent,
      categoryId: categoryId.value
    })
    title.value = ''
    content.value = ''
    await refreshDocuments()
  } finally {
    creating.value = false
  }
}

function handleFileChange(event: Event) {
  const input = event.target as HTMLInputElement
  selectedFile.value = input.files?.[0] ?? null
  uploadMessage.value = ''
}

async function submitUpload() {
  if (!selectedFile.value || uploading.value) {
    return
  }

  uploading.value = true
  uploadMessage.value = ''

  try {
    await uploadKnowledgeDocument({
      file: selectedFile.value,
      title: title.value.trim() || undefined,
      categoryId: categoryId.value
    })
    title.value = ''
    content.value = ''
    selectedFile.value = null
    uploadMessage.value = '文件已上传并开始解析'
    await refreshDocuments()
  } finally {
    uploading.value = false
  }
}

async function submitSearch() {
  const normalizedQuery = query.value.trim()

  if (!normalizedQuery || searching.value) {
    return
  }

  searching.value = true

  try {
    results.value = await searchKnowledge({
      query: normalizedQuery,
      topK: topK.value,
      minSimilarity: minSimilarity.value
    })
  } finally {
    searching.value = false
  }
}

onMounted(loadDocuments)
</script>

<template>
  <section class="knowledge-view">
    <header class="workspace-page-header">
      <div>
        <p>Knowledge workspace</p>
        <h3>知识库管理</h3>
      </div>
      <span>{{ documentCountText }} · 文本录入 · 检索验证</span>
    </header>

    <div class="knowledge-layout">
      <section class="knowledge-panel document-list-panel">
        <div class="panel-heading">
          <span>文档列表</span>
          <strong>{{ documents.length }}</strong>
        </div>
        <LoadingState v-if="loading" message="正在加载知识文档" />
        <ErrorState v-else-if="error" :message="error" />
        <EmptyState v-else-if="documents.length === 0" message="暂无知识文档" />
        <article v-for="document in documents" v-else :key="document.id" class="knowledge-document-item">
          <div>
            <h4>{{ document.title }}</h4>
            <span>{{ document.enabled ? '启用' : '停用' }}</span>
          </div>
          <dl>
            <div>
              <dt>分类</dt>
              <dd>{{ document.categoryId ?? '默认' }}</dd>
            </div>
            <div>
              <dt>解析</dt>
              <dd>{{ document.parseStatus }}</dd>
            </div>
            <div>
              <dt>重试</dt>
              <dd>{{ document.retryCount ?? 0 }}</dd>
            </div>
          </dl>
          <p v-if="document.parseError">{{ document.parseError }}</p>
        </article>
      </section>

      <section class="knowledge-panel">
        <div class="panel-heading">
          <span>文本录入</span>
        </div>
        <form class="knowledge-form" @submit.prevent="submitDocument">
          <label>
            标题
            <input v-model="title" data-testid="document-title" type="text" placeholder="例如：账号手册" />
          </label>
          <label>
            分类 ID
            <input v-model.number="categoryId" type="number" min="1" />
          </label>
          <label>
            正文
            <textarea
              v-model="content"
              data-testid="document-content"
              rows="8"
              placeholder="粘贴知识库正文，系统会切分并向量化。"
            />
          </label>
          <button data-testid="create-document" type="button" :disabled="creating" @click="submitDocument">
            {{ creating ? '录入中...' : '录入知识' }}
          </button>
        </form>

        <div class="knowledge-form upload-form">
          <label>
            文件上传
            <input
              data-testid="document-file"
              type="file"
              accept=".txt,.md,.markdown,text/plain,text/markdown"
              @change="handleFileChange"
            />
          </label>
          <button data-testid="upload-document" type="button" :disabled="uploading || !selectedFile" @click="submitUpload">
            {{ uploading ? '上传中...' : '上传文件' }}
          </button>
          <p v-if="uploadMessage" class="form-hint">{{ uploadMessage }}</p>
        </div>
      </section>

      <section class="knowledge-panel">
        <div class="panel-heading">
          <span>检索测试</span>
          <strong>{{ results.length }}</strong>
        </div>
        <form class="knowledge-form search-form" @submit.prevent="submitSearch">
          <label>
            查询
            <input v-model="query" data-testid="search-query" type="text" placeholder="例如：重置密码" />
          </label>
          <div class="knowledge-form-grid">
            <label>
              Top K
              <select v-model.number="topK">
                <option :value="3">3</option>
                <option :value="5">5</option>
                <option :value="8">8</option>
              </select>
            </label>
            <label>
              相似度下限
              <select v-model.number="minSimilarity">
                <option :value="0">0</option>
                <option :value="0.2">0.2</option>
                <option :value="0.4">0.4</option>
              </select>
            </label>
          </div>
          <button data-testid="search-button" type="button" :disabled="searching" @click="submitSearch">
            {{ searching ? '检索中...' : '检索知识' }}
          </button>
        </form>

        <EmptyState v-if="results.length === 0" message="输入查询后查看向量检索命中的知识片段" />
        <article v-for="result in results" v-else :key="result.chunkId" class="knowledge-result-item">
          <div>
            <h4>{{ result.sourceTitle }}</h4>
            <span>{{ similarityText(result.similarity) }}</span>
          </div>
          <p>{{ result.content }}</p>
        </article>
      </section>
    </div>
  </section>
</template>
