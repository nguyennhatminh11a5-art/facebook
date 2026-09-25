import axios, { AxiosError, type AxiosRequestConfig, type InternalAxiosRequestConfig } from 'axios'
import type { AuthResponse } from './types'

/** Access token chỉ nằm trong bộ nhớ (CLAUDE.md mục 6). Tải lại trang → gọi /auth/refresh. */
let accessToken: string | null = null
let refreshPromise: Promise<AuthResponse> | null = null
let onAuthFailure: () => void = () => {}

export function setAccessToken(token: string | null) {
  accessToken = token
}

export function getAccessToken() {
  return accessToken
}

/** Gọi khi refresh thất bại (phiên hết hạn/bị thu hồi) → đưa về trang đăng nhập. */
export function setAuthFailureHandler(handler: () => void) {
  onAuthFailure = handler
}

export const api = axios.create({
  baseURL: '/api/v1',
  withCredentials: true,
})

const PUBLIC_AUTH_PATHS = ['/auth/login', '/auth/register', '/auth/refresh']

function isPublicAuthPath(url: string | undefined) {
  return PUBLIC_AUTH_PATHS.some((path) => url?.startsWith(path))
}

/**
 * Refresh dùng chung: mọi lời gọi đồng thời trong tab chờ cùng một request (single-flight), để
 * không xoay vòng refresh token nhiều lần.
 */
export function refreshSession(): Promise<AuthResponse> {
  refreshPromise ??= api
    .post<AuthResponse>('/auth/refresh')
    .then((res) => {
      setAccessToken(res.data.accessToken)
      return res.data
    })
    .finally(() => {
      refreshPromise = null
    })
  return refreshPromise
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  if (accessToken && !isPublicAuthPath(config.url)) {
    config.headers.set('Authorization', `Bearer ${accessToken}`)
  }
  return config
})

type RetryableConfig = AxiosRequestConfig & { _retried?: boolean }

api.interceptors.response.use(
  (response) => response,
  async (error: AxiosError) => {
    const config = error.config as RetryableConfig | undefined
    if (
      error.response?.status !== 401 ||
      !config ||
      config._retried ||
      isPublicAuthPath(config.url)
    ) {
      throw error
    }
    config._retried = true
    try {
      await refreshSession()
    } catch {
      setAccessToken(null)
      onAuthFailure()
      throw error
    }
    return api(config)
  },
)
