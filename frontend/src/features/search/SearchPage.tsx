import { useInfiniteQuery } from '@tanstack/react-query'
import { Search } from 'lucide-react'
import { useDeferredValue } from 'react'
import { useSearchParams } from 'react-router'
import { searchUsers } from '@/api/users'
import { InfiniteSentinel } from '@/components/InfiniteSentinel'
import { EmptyState, ErrorState } from '@/components/states'
import { UserRow } from '@/components/UserRow'
import { Card } from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { queryKeys } from '@/lib/queryKeys'

const MIN_QUERY = 2
const MAX_QUERY = 50

export function SearchPage() {
  const [params, setParams] = useSearchParams()
  const input = params.get('q') ?? ''
  const q = useDeferredValue(input.trim())
  const enabled = q.length >= MIN_QUERY

  const query = useInfiniteQuery({
    queryKey: queryKeys.search(q),
    queryFn: ({ pageParam }) => searchUsers(q, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
    enabled,
  })

  return (
    <div className="grid gap-4">
      <h1 className="text-xl font-bold">Tìm kiếm</h1>
      <div className="relative">
        <Search
          className="pointer-events-none absolute top-1/2 left-3 size-4 -translate-y-1/2 text-muted-foreground"
          aria-hidden="true"
        />
        <Input
          type="search"
          aria-label="Tìm người theo tên"
          placeholder="Nhập tên (không cần dấu)…"
          className="pl-9"
          value={input}
          maxLength={MAX_QUERY}
          onChange={(e) =>
            setParams(e.target.value ? { q: e.target.value } : {}, { replace: true })
          }
        />
      </div>

      {!enabled && (
        <EmptyState title="Tìm bạn bè">Nhập ít nhất {MIN_QUERY} ký tự để tìm kiếm.</EmptyState>
      )}
      {enabled && query.isPending && (
        <Card className="grid gap-3 p-4" role="status" aria-label="Đang tìm">
          <Skeleton className="h-10" />
          <Skeleton className="h-10" />
        </Card>
      )}
      {enabled && query.isError && <ErrorState onRetry={() => query.refetch()} />}
      {enabled && query.isSuccess && <Results pages={query.data.pages} q={q} query={query} />}
    </div>
  )
}

function Results({
  pages,
  q,
  query,
}: {
  pages: Awaited<ReturnType<typeof searchUsers>>[]
  q: string
  query: { hasNextPage: boolean; isFetchingNextPage: boolean; fetchNextPage: () => unknown }
}) {
  const users = pages.flatMap((p) => p.items)
  if (users.length === 0) {
    return <EmptyState title={`Không tìm thấy ai tên "${q}"`}>Thử từ khoá khác.</EmptyState>
  }
  return (
    <Card className="grid gap-4 p-4">
      {users.map((u) => (
        <UserRow key={u.id} user={u} />
      ))}
      <InfiniteSentinel
        hasNextPage={query.hasNextPage}
        isFetchingNextPage={query.isFetchingNextPage}
        fetchNextPage={query.fetchNextPage}
      />
    </Card>
  )
}
