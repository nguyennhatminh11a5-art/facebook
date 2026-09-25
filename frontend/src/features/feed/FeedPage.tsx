import { useInfiniteQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { fetchFeed } from '@/api/posts'
import { EmptyState } from '@/components/states'
import { PostComposer } from '@/features/posts/PostComposer'
import { PostList } from '@/features/posts/PostList'
import { queryKeys } from '@/lib/queryKeys'

export function FeedPage() {
  const query = useInfiniteQuery({
    queryKey: queryKeys.feed,
    queryFn: ({ pageParam }) => fetchFeed(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })

  return (
    <div className="grid gap-4">
      <h1 className="sr-only">Bảng tin</h1>
      <PostComposer />
      <PostList
        query={query}
        empty={
          <EmptyState title="Bảng tin còn trống">
            Hãy đăng bài đầu tiên, hoặc{' '}
            <Link to="/search" className="text-primary hover:underline">
              tìm bạn bè
            </Link>{' '}
            để xem bài của họ.
          </EmptyState>
        }
      />
    </div>
  )
}
