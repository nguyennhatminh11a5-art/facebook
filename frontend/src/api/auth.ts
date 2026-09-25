import { api, setAccessToken } from './client'
import type { AuthResponse, Me } from './types'

export type RegisterInput = { email: string; password: string; displayName: string }
export type LoginInput = { email: string; password: string }

export async function register(input: RegisterInput): Promise<Me> {
  const res = await api.post<Me>('/auth/register', input)
  return res.data
}

export async function login(input: LoginInput): Promise<AuthResponse> {
  const res = await api.post<AuthResponse>('/auth/login', input)
  setAccessToken(res.data.accessToken)
  return res.data
}

export async function logout(): Promise<void> {
  try {
    await api.post('/auth/logout')
  } finally {
    setAccessToken(null)
  }
}
