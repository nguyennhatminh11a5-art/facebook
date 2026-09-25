import type { InfiniteData, QueryClient, QueryKey } from '@tanstack/react-query'
import type { Page, Post } from '@/api/types'
import { POST_QUERY_ROOTS } from './queryKeys'

type PostPage = Page<Post>
type Snapshot = Array<[QueryKey, unknown]>

const isPostQuery = (key: QueryKey) => (POST_QUERY_ROOTS as readonly unknown[]).includes(key[0])

function isInfinite(data: unknown): data is InfiniteData<PostPage> {
  return typeof data === 'object' && data !== null && 'pages' in data
}

function isPost(data: unknown): data is Post {
  return typeof data === 'object' && data !== null && 'author' in data && 'id' in data
}

/** Áp {@code updater} cho bài {@code postId} trong mọi cache chứa bài (feed, trang cá nhân, chi tiết). */
export function updatePostInCaches(
  queryClient: QueryClient,
  postId: number,
  updater: (post: Post) => Post,
) {
  queryClient.setQueriesData({ predicate: (q) => isPostQuery(q.queryKey) }, (data: unknown) => {
    if (isInfinite(data)) {
      return {
        ...data,
        pages: data.pages.map((page) => ({
          ...page,
          items: page.items.map((p) => (p.id === postId ? updater(p) : p)),
        })),
      }
    }
    if (isPost(data) && data.id === postId) return updater(data)
    return data
  })
}

export function removePostFromCaches(queryClient: QueryClient, postId: number) {
  queryClient.setQueriesData({ predicate: (q) => isPostQuery(q.queryKey) }, (data: unknown) => {
    if (isInfinite(data)) {
      return {
        ...data,
        pages: data.pages.map((page) => ({
          ...page,
          items: page.items.filter((p) => p.id !== postId),
        })),
      }
    }
    return data
  })
  queryClient.removeQueries({ queryKey: ['post', postId] })
}

export async function snapshotPostCaches(queryClient: QueryClient): Promise<Snapshot> {
  await queryClient.cancelQueries({ predicate: (q) => isPostQuery(q.queryKey) })
  return queryClient.getQueriesData({ predicate: (q) => isPostQuery(q.queryKey) })
}

export function restoreSnapshot(queryClient: QueryClient, snapshot: Snapshot) {
  snapshot.forEach(([key, data]) => queryClient.setQueryData(key, data))
}

export function invalidatePostQueries(queryClient: QueryClient) {
  return queryClient.invalidateQueries({ predicate: (q) => isPostQuery(q.queryKey) })
}
