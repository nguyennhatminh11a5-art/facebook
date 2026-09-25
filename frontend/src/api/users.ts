import { api } from './client'
import type { Me, Page, Profile, UserSummary } from './types'

export async function fetchProfile(id: number): Promise<Profile> {
  const res = await api.get<Profile>(`/users/${id}`)
  return res.data
}

export async function updateMe(input: { displayName: string; bio: string }): Promise<Me> {
  const res = await api.patch<Me>('/users/me', input)
  return res.data
}

export async function uploadAvatar(file: File): Promise<Me> {
  const form = new FormData()
  form.append('file', file)
  const res = await api.post<Me>('/users/me/avatar', form)
  return res.data
}

export async function searchUsers(q: string, cursor: string | null): Promise<Page<UserSummary>> {
  const res = await api.get<Page<UserSummary>>('/users/search', {
    params: { q, cursor: cursor ?? undefined },
  })
  return res.data
}
