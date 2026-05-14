import request from './request'
import type { ApiResponse } from '../types/auth'
import type {
  AdminChatRecordDetail,
  AdminChatRecordListItem,
  AdminDashboard,
  AdminFeedbackItem,
  AdminListParams,
  AdminPage,
  AdminUnmatchedQuestion,
} from '../types/admin'

type BackendPage<T> = {
  list?: T[]
  records?: T[]
  total?: number
  pageNum?: number
  page?: number
  pageSize?: number
}

function paramsOf(params: AdminListParams) {
  const searchParams = new URLSearchParams()

  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== null && String(value).trim() !== '') {
      searchParams.set(key, String(value).trim())
    }
  })

  return searchParams
}

function normalizePage<T>(data: BackendPage<T> | null, fallback: AdminListParams): AdminPage<T> {
  return {
    records: data?.records ?? data?.list ?? [],
    total: data?.total ?? 0,
    page: data?.page ?? data?.pageNum ?? fallback.page ?? 1,
    pageSize: data?.pageSize ?? fallback.pageSize ?? 20,
  }
}

function unwrap<T>(response: ApiResponse<T>) {
  if (response.code !== 0 || response.data === null) {
    throw new Error(response.message || '请求失败')
  }

  return response.data
}

export async function getAdminDashboard() {
  const response = await request.get<ApiResponse<AdminDashboard>, ApiResponse<AdminDashboard>>(
    '/admin/dashboard',
  )
  return unwrap(response)
}

export async function getAdminChatRecords(params: AdminListParams) {
  const response = await request.get<ApiResponse<BackendPage<AdminChatRecordListItem>>, ApiResponse<BackendPage<AdminChatRecordListItem>>>(
    '/admin/chat-records',
    { params: paramsOf(params) },
  )
  return normalizePage(unwrap(response), params)
}

export async function getAdminChatRecordDetail(id: number) {
  const response = await request.get<ApiResponse<AdminChatRecordDetail>, ApiResponse<AdminChatRecordDetail>>(
    `/admin/chat-records/${encodeURIComponent(String(id))}`,
  )
  return unwrap(response)
}

export async function getAdminFeedback(params: AdminListParams) {
  const response = await request.get<ApiResponse<BackendPage<AdminFeedbackItem>>, ApiResponse<BackendPage<AdminFeedbackItem>>>(
    '/admin/feedback',
    { params: paramsOf(params) },
  )
  return normalizePage(unwrap(response), params)
}

export async function getAdminUnmatchedQuestions(params: AdminListParams) {
  const response = await request.get<ApiResponse<BackendPage<AdminUnmatchedQuestion>>, ApiResponse<BackendPage<AdminUnmatchedQuestion>>>(
    '/admin/unmatched-questions',
    { params: paramsOf(params) },
  )
  return normalizePage(unwrap(response), params)
}
