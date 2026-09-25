import { api } from './client'
import type { Comment, Page } from './types'

export async function fetchComments(postId: number, cursor: string | null): Promise<Page<Comment>> {
  const res = await api.get<Page<Comment>>(`/posts/${postId}/comments`, {
    params: { cursor: cursor ?? undefined },
  })
  return res.data
}

export async function addComment(postId: number, content: string): Promise<Comment> {
  const res = await api.post<Comment>(`/posts/${postId}/comments`, { content })
  return res.data
}

export async function deleteComment(commentId: number): Promise<void> {
  await api.delete(`/comments/${commentId}`)
}
