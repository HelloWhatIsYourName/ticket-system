import { http, unwrapData, type ApiResponse } from './http'

export type TicketPriority = 'LOW' | 'NORMAL' | 'HIGH' | 'URGENT'
export type TicketStatus = 'PENDING_ASSIGN' | 'PENDING_PROCESS' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
export type TicketSource = 'MANUAL' | 'AI_SESSION'
export type TicketCommentType = 'USER_REPLY' | 'AGENT_REPLY' | 'INTERNAL_NOTE' | 'SYSTEM'
export type SlaStatus = 'COMPLETED' | 'OVERDUE' | 'DUE_SOON' | 'ON_TRACK'

export interface TicketCategory {
  id: number
  name: string
  parentId?: number | null
  sortOrder?: number | null
  enabled: boolean
  createdAt?: string
  updatedAt?: string
}

export interface TicketSummary {
  id: number
  ticketNo: string
  title: string
  description?: string
  status: TicketStatus
  priority?: TicketPriority | null
  categoryId?: number | null
  creatorId?: number
  assigneeId?: number | null
  source?: TicketSource
  sourceSessionId?: number | null
  sourceMessageId?: number | null
  aiSummary?: string | null
  aiSuggestion?: string | null
  transferReason?: string | null
  deadlineAt?: string | null
  slaStatus?: SlaStatus | null
  slaRemainingMinutes?: number | null
  reopenCount?: number
  firstResolvedAt?: string | null
  closedAt?: string | null
  createdAt?: string
  updatedAt?: string
}

export interface TicketFlowLog {
  id: number
  ticketId: number
  fromStatus?: TicketStatus | null
  toStatus?: TicketStatus | null
  action: string
  operatorId: number
  commentText?: string | null
  remark?: string | null
  createdAt?: string
}

export interface TicketDetail extends TicketSummary {
  flowLogs: TicketFlowLog[]
}

export interface TicketComment {
  id: number
  ticketId: number
  authorId: number
  commentType: TicketCommentType
  content: string
  internal: boolean
  createdAt?: string
}

export interface CreateTicketFromAiSessionRequest {
  sessionId: number
  assistantMessageId?: number
  title: string
  description: string
  categoryId?: number
  priority?: TicketPriority
  transferReason?: string
}

export interface CreateTicketCommentRequest {
  commentType: TicketCommentType
  content: string
}

export interface TicketActionRequest {
  comment?: string
}

export interface AssignTicketRequest extends TicketActionRequest {
  assigneeId: number
}

export interface AssignmentRecommendation {
  recommendedAssigneeId?: number | null
  recommendedUsername?: string | null
  recommendedDisplayName?: string | null
  activeTicketCount?: number | null
  reason: string
}

export async function listTicketCategories(includeDisabled = false): Promise<TicketCategory[]> {
  const response = await http.get<ApiResponse<TicketCategory[]>>('/ticket-categories', {
    params: { includeDisabled }
  })

  return unwrapData(response.data)
}

export async function createTicketFromAiSession(request: CreateTicketFromAiSessionRequest): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>('/tickets/from-ai-session', request)

  return unwrapData(response.data)
}

export async function listMyTickets(): Promise<TicketSummary[]> {
  const response = await http.get<ApiResponse<TicketSummary[]>>('/tickets/my')

  return unwrapData(response.data)
}

export async function listAssignedTickets(): Promise<TicketSummary[]> {
  const response = await http.get<ApiResponse<TicketSummary[]>>('/tickets/assigned')

  return unwrapData(response.data)
}

export async function listManagedTickets(): Promise<TicketSummary[]> {
  const response = await http.get<ApiResponse<TicketSummary[]>>('/tickets/manage')

  return unwrapData(response.data)
}

export async function getTicket(ticketId: number): Promise<TicketDetail> {
  const response = await http.get<ApiResponse<TicketDetail>>(`/tickets/${ticketId}`)

  return unwrapData(response.data)
}

export async function getAssignmentRecommendation(ticketId: number): Promise<AssignmentRecommendation> {
  const response = await http.get<ApiResponse<AssignmentRecommendation>>(
    `/tickets/${ticketId}/assignment-recommendation`
  )

  return unwrapData(response.data)
}

export async function listTicketComments(ticketId: number): Promise<TicketComment[]> {
  const response = await http.get<ApiResponse<TicketComment[]>>(`/tickets/${ticketId}/comments`)

  return unwrapData(response.data)
}

export async function createTicketComment(
  ticketId: number,
  request: CreateTicketCommentRequest
): Promise<TicketComment> {
  const response = await http.post<ApiResponse<TicketComment>>(`/tickets/${ticketId}/comments`, request)

  return unwrapData(response.data)
}

export async function assignTicket(ticketId: number, request: AssignTicketRequest): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/assign`, request)

  return unwrapData(response.data)
}

export async function startTicket(ticketId: number, comment?: string): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/start`, { comment })

  return unwrapData(response.data)
}

export async function resolveTicket(ticketId: number, comment?: string): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/resolve`, { comment })

  return unwrapData(response.data)
}

export async function reopenTicket(ticketId: number, comment?: string): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/reopen`, { comment })

  return unwrapData(response.data)
}

export async function confirmCloseTicket(ticketId: number, comment?: string): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/confirm-close`, { comment })

  return unwrapData(response.data)
}

export async function closeTicket(ticketId: number, comment?: string): Promise<TicketSummary> {
  const response = await http.post<ApiResponse<TicketSummary>>(`/tickets/${ticketId}/close`, { comment })

  return unwrapData(response.data)
}
