import { useQuery } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router'
import { fetchPost } from '@/api/posts'
import { ErrorState } from '@/components/states'
import { httpStatus } from '@/lib/errors'
import { queryKeys } from '@/lib/queryKeys'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { PostCard } from './PostCard'
import { PostSkeleton } from './PostSkeleton'

export function PostPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const query = useQuery({
    queryKey: queryKeys.post(id),
    queryFn: () => fetchPost(id),
    enabled: Number.isInteger(id) && id > 0,
  })

  if (!Number.isInteger(id) || id <= 0) return <NotFoundPage />
  if (query.isPending) return <PostSkeleton />
  if (query.isError) {
    if (httpStatus(query.error) === 404) return <NotFoundPage />
    return <ErrorState onRetry={() => query.refetch()} />
  }
  return <PostCard post={query.data} defaultShowComments onDeleted={() => navigate('/')} />
}
