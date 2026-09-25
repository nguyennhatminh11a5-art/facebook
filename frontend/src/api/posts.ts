import { api } from './client'
import type { Page, Post, ReactionType, UserPostsPage } from './types'

export async function fetchFeed(cursor: string | null): Promise<Page<Post>> {
  const res = await api.get<Page<Post>>('/feed', { params: { cursor: cursor ?? undefined } })
  return res.data
}

export async function fetchUserPosts(
  userId: number,
  cursor: string | null,
): Promise<UserPostsPage> {
  const res = await api.get<UserPostsPage>(`/users/${userId}/posts`, {
    params: { cursor: cursor ?? undefined },
  })
  return res.data
}

export async function fetchPost(id: number): Promise<Post> {
  const res = await api.get<Post>(`/posts/${id}`)
  return res.data
}

export async function createPost(input: { content: string; images: File[] }): Promise<Post> {
  const form = new FormData()
  if (input.content.trim()) form.append('content', input.content)
  input.images.forEach((file) => form.append('images', file))
  const res = await api.post<Post>('/posts', form)
  return res.data
}

export async function updatePost(id: number, content: string): Promise<Post> {
  const res = await api.patch<Post>(`/posts/${id}`, { content })
  return res.data
}

export async function deletePost(id: number): Promise<void> {
  await api.delete(`/posts/${id}`)
}

export type ReactionSummary = { reactionCount: number; myReaction: ReactionType | null }

export async function react(postId: number, type: ReactionType): Promise<ReactionSummary> {
  const res = await api.put<ReactionSummary>(`/posts/${postId}/reaction`, { type })
  return res.data
}

export async function unreact(postId: number): Promise<ReactionSummary> {
  const res = await api.delete<ReactionSummary>(`/posts/${postId}/reaction`)
  return res.data
}
