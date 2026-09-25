import { api } from './client'
import type { FriendRequest, FriendStatus, FriendSuggestion, Page, UserSummary } from './types'

export type FriendshipStatus = { userId: number; friendStatus: FriendStatus }

export async function fetchFriends(cursor: string | null): Promise<Page<UserSummary>> {
  const res = await api.get<Page<UserSummary>>('/friends', {
    params: { cursor: cursor ?? undefined },
  })
  return res.data
}

export async function fetchRequests(direction: 'incoming' | 'outgoing'): Promise<FriendRequest[]> {
  const res = await api.get<FriendRequest[]>('/friends/requests', { params: { direction } })
  return res.data
}

export async function fetchSuggestions(): Promise<FriendSuggestion[]> {
  const res = await api.get<FriendSuggestion[]>('/friends/suggestions')
  return res.data
}

export async function sendRequest(userId: number): Promise<FriendshipStatus> {
  const res = await api.post<FriendshipStatus>('/friends/requests', { userId })
  return res.data
}

export async function acceptRequest(userId: number): Promise<FriendshipStatus> {
  const res = await api.post<FriendshipStatus>(`/friends/requests/${userId}/accept`)
  return res.data
}

export async function deleteRequest(userId: number): Promise<void> {
  await api.delete(`/friends/requests/${userId}`)
}

export async function unfriend(userId: number): Promise<void> {
  await api.delete(`/friends/${userId}`)
}
