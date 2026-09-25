import type { InfiniteData } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import type { Page, Post } from '@/api/types'
import { InfiniteSentinel } from '@/components/InfiniteSentinel'
import { ErrorState } from '@/components/states'
import { PostCard } from './PostCard'
import { PostListSkeleton } from './PostSkeleton'

/** Phần của kết quả useInfiniteQuery mà danh sách bài cần (feed và trang cá nhân dùng chung). */
type PostListQuery = {
  data: InfiniteData<Page<Post>, unknown> | undefined
  isPending: boolean
  isError: boolean
  refetch: () => unknown
  hasNextPage: boolean
  isFetchingNextPage: boolean
  isFetchNextPageError: boolean
  fetchNextPage: () => unknown
}

/** Danh sách bài cuộn vô hạn với đủ 3 trạng thái: đang tải, lỗi, rỗng. */
export function PostList({ query, empty }: { query: PostListQuery; empty: ReactNode }) {
  if (query.isPending) return <PostListSkeleton />
  if (query.isError || !query.data) {
    return <ErrorState message="Không tải được bài viết" onRetry={() => query.refetch()} />
  }
  const posts = query.data.pages.flatMap((p) => p.items)
  if (posts.length === 0) return <>{empty}</>

  return (
    <div className="grid gap-4">
      {posts.map((post) => (
        <PostCard key={post.id} post={post} />
      ))}
      <InfiniteSentinel
        hasNextPage={query.hasNextPage}
        isFetchingNextPage={query.isFetchingNextPage}
        fetchNextPage={query.fetchNextPage}
        label="Tải thêm bài viết"
      />
      {query.isFetchNextPageError && (
        <ErrorState
          message="Không tải được trang tiếp theo"
          onRetry={() => query.fetchNextPage()}
        />
      )}
    </div>
  )
}
