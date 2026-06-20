import { http, unwrapData, type ApiResponse } from './http'

export type KnowledgeParseStatus = 'PENDING_PARSE' | 'PARSING' | 'PARSED' | 'FAILED'

export interface KnowledgeDocument {
  id: number
  title: string
  categoryId?: number | null
  enabled: boolean
  parseStatus: KnowledgeParseStatus | string
  parseError?: string | null
  retryCount?: number | null
}

export interface KnowledgeChunk {
  id: number
  chunkIndex: number
  content: string
  contentHash?: string
  sourceTitle: string
}

export interface KnowledgeSearchResult {
  chunkId: number
  documentId: number
  categoryId?: number | null
  chunkIndex?: number
  content: string
  sourceTitle: string
  distance?: number
  similarity?: number
}

export interface CreateTextDocumentRequest {
  title: string
  categoryId?: number
  content: string
}

export interface UploadKnowledgeDocumentRequest {
  file: File
  title?: string
  categoryId?: number
}

export interface SearchKnowledgeRequest {
  query: string
  categoryId?: number
  topK?: number
  minSimilarity?: number
}

export async function listDocuments(): Promise<KnowledgeDocument[]> {
  const response = await http.get<ApiResponse<KnowledgeDocument[]>>('/kb/documents')

  return unwrapData(response.data)
}

export async function createTextDocument(request: CreateTextDocumentRequest): Promise<KnowledgeDocument> {
  const response = await http.post<ApiResponse<KnowledgeDocument>>('/kb/documents/text', request)

  return unwrapData(response.data)
}

export async function uploadKnowledgeDocument(request: UploadKnowledgeDocumentRequest): Promise<KnowledgeDocument> {
  const formData = new FormData()
  formData.append('file', request.file)
  if (request.title) {
    formData.append('title', request.title)
  }
  if (request.categoryId !== undefined) {
    formData.append('categoryId', String(request.categoryId))
  }

  const response = await http.post<ApiResponse<KnowledgeDocument>>('/kb/documents/upload', formData)

  return unwrapData(response.data)
}

export async function getDocument(documentId: number): Promise<KnowledgeDocument> {
  const response = await http.get<ApiResponse<KnowledgeDocument>>(`/kb/documents/${documentId}`)

  return unwrapData(response.data)
}

export async function enableDocument(documentId: number): Promise<KnowledgeDocument> {
  const response = await http.post<ApiResponse<KnowledgeDocument>>(`/kb/documents/${documentId}/enable`)

  return unwrapData(response.data)
}

export async function disableDocument(documentId: number): Promise<KnowledgeDocument> {
  const response = await http.post<ApiResponse<KnowledgeDocument>>(`/kb/documents/${documentId}/disable`)

  return unwrapData(response.data)
}

export async function retryParseDocument(documentId: number): Promise<KnowledgeDocument> {
  const response = await http.post<ApiResponse<KnowledgeDocument>>(`/kb/documents/${documentId}/retry-parse`)

  return unwrapData(response.data)
}

export async function listDocumentChunks(documentId: number): Promise<KnowledgeChunk[]> {
  const response = await http.get<ApiResponse<KnowledgeChunk[]>>(`/kb/documents/${documentId}/chunks`)

  return unwrapData(response.data)
}

export async function searchKnowledge(request: SearchKnowledgeRequest): Promise<KnowledgeSearchResult[]> {
  const response = await http.post<ApiResponse<KnowledgeSearchResult[]>>('/kb/search', request)

  return unwrapData(response.data)
}
