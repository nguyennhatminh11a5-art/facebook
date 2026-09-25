import { QueryClient, type InfiniteData } from '@tanstack/react-query'
import { describe, expect, it } from 'vitest'
import type { Page, Post } from '@/api/types'
import { applyReaction } from '@/features/posts/reactions'
import {
  removePostFromCaches,
  restoreSnapshot,
  snapshotPostCaches,
  updatePostInCaches,
} from './postCache'

function post(id: number, overrides: Partial<Post> = {}): Post {
  return {
    id,
    author: { id: 1, displayName: 'A', avatarUrl: null },
    content: `bài ${id}`,
    images: [],
    reactionCount: 0,
    commentCount: 0,
    myReaction: null,
    createdAt: '2026-01-01T00:00:00Z',
    updatedAt: '2026-01-01T00:00:00Z',
    editable: true,
    ...overrides,
  }
}

function infinite(...pages: Post[][]): InfiniteData<Page<Post>, string | null> {
  return {
    pages: pages.map((items) => ({ items, nextCursor: null })),
    pageParams: pages.map(() => null),
  }
}

describe('applyReaction', () => {
  it('adds, changes and removes with correct count', () => {
    const p = post(1, { reactionCount: 3 })
    const liked = applyReaction(p, 'LIKE')
    expect(liked).toMatchObject({ myReaction: 'LIKE', reactionCount: 4 })
    const loved = applyReaction(liked, 'LOVE')
    expect(loved).toMatchObject({ myReaction: 'LOVE', reactionCount: 4 })
    expect(applyReaction(loved, null)).toMatchObject({ myReaction: null, reactionCount: 3 })
    expect(applyReaction(post(2), null).reactionCount).toBe(0)
  })
})

describe('post caches', () => {
  it('updates the post everywhere and restores snapshot', async () => {
    const qc = new QueryClient()
    qc.setQueryData(['feed'], infinite([post(1), post(2)], [post(3)]))
    qc.setQueryData(['userPosts', 1], infinite([post(2)]))
    qc.setQueryData(['post', 2], post(2))
    qc.setQueryData(['comments', 2], { unrelated: true })

    const snapshot = await snapshotPostCaches(qc)
    updatePostInCaches(qc, 2, (p) => ({ ...p, commentCount: 9 }))

    const feed = qc.getQueryData<InfiniteData<Page<Post>>>(['feed'])
    expect(feed?.pages[0]?.items[1]?.commentCount).toBe(9)
    expect(feed?.pages[0]?.items[0]?.commentCount).toBe(0)
    expect(
      qc.getQueryData<InfiniteData<Page<Post>>>(['userPosts', 1])?.pages[0]?.items[0]?.commentCount,
    ).toBe(9)
    expect(qc.getQueryData<Post>(['post', 2])?.commentCount).toBe(9)
    expect(qc.getQueryData(['comments', 2])).toEqual({ unrelated: true })

    restoreSnapshot(qc, snapshot)
    expect(qc.getQueryData<Post>(['post', 2])?.commentCount).toBe(0)
  })

  it('removes a deleted post from lists', () => {
    const qc = new QueryClient()
    qc.setQueryData(['feed'], infinite([post(1), post(2)]))
    qc.setQueryData(['post', 2], post(2))
    removePostFromCaches(qc, 2)
    const feed = qc.getQueryData<InfiniteData<Page<Post>>>(['feed'])
    expect(feed?.pages[0]?.items.map((p) => p.id)).toEqual([1])
    expect(qc.getQueryData(['post', 2])).toBeUndefined()
  })
})
