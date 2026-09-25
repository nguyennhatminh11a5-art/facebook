import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { useSearchParams } from 'react-router'
import { fetchFriends, fetchRequests, fetchSuggestions } from '@/api/friends'
import { InfiniteSentinel } from '@/components/InfiniteSentinel'
import { EmptyState, ErrorState } from '@/components/states'
import { UserRow } from '@/components/UserRow'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { queryKeys } from '@/lib/queryKeys'
import { formatRelative } from '@/lib/time'
import { cn } from '@/lib/utils'
import { FriendActionButton } from './FriendActionButton'

const TABS = [
  { id: 'friends', label: 'Bạn bè' },
  { id: 'incoming', label: 'Lời mời' },
  { id: 'outgoing', label: 'Đã gửi' },
  { id: 'suggestions', label: 'Gợi ý' },
] as const
type TabId = (typeof TABS)[number]['id']

function ListSkeleton() {
  return (
    <Card className="grid gap-4 p-4" role="status" aria-label="Đang tải">
      {Array.from({ length: 4 }, (_, i) => (
        <div key={i} className="flex items-center gap-3">
          <Skeleton className="size-10 rounded-full" />
          <Skeleton className="h-4 w-40" />
        </div>
      ))}
    </Card>
  )
}

export function FriendsPage() {
  const [params, setParams] = useSearchParams()
  const current = (TABS.find((t) => t.id === params.get('tab'))?.id ?? 'friends') as TabId

  return (
    <div className="grid gap-4">
      <h1 className="text-xl font-bold">Bạn bè</h1>
      <div role="tablist" aria-label="Bạn bè" className="flex gap-1 overflow-x-auto">
        {TABS.map((tab) => (
          <button
            key={tab.id}
            role="tab"
            type="button"
            aria-selected={current === tab.id}
            onClick={() => setParams(tab.id === 'friends' ? {} : { tab: tab.id })}
            className={cn(
              'rounded-full px-4 py-1.5 text-sm font-medium whitespace-nowrap hover:bg-accent',
              current === tab.id && 'bg-primary/10 text-primary',
            )}
          >
            {tab.label}
          </button>
        ))}
      </div>
      <div role="tabpanel">
        {current === 'friends' && <FriendList />}
        {(current === 'incoming' || current === 'outgoing') && <RequestList direction={current} />}
        {current === 'suggestions' && <SuggestionList />}
      </div>
    </div>
  )
}

function FriendList() {
  const query = useInfiniteQuery({
    queryKey: queryKeys.friends,
    queryFn: ({ pageParam }) => fetchFriends(pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
  if (query.isPending) return <ListSkeleton />
  if (query.isError) return <ErrorState onRetry={() => query.refetch()} />
  const friends = query.data.pages.flatMap((p) => p.items)
  if (friends.length === 0) {
    return <EmptyState title="Chưa có bạn bè">Dùng Tìm kiếm hoặc tab Gợi ý để kết bạn.</EmptyState>
  }
  return (
    <Card className="grid gap-4 p-4">
      {friends.map((u) => (
        <UserRow
          key={u.id}
          user={u}
          action={<FriendActionButton userId={u.id} status="FRIENDS" size="sm" />}
        />
      ))}
      <InfiniteSentinel
        hasNextPage={query.hasNextPage}
        isFetchingNextPage={query.isFetchingNextPage}
        fetchNextPage={query.fetchNextPage}
      />
    </Card>
  )
}

function RequestList({ direction }: { direction: 'incoming' | 'outgoing' }) {
  const query = useQuery({
    queryKey: queryKeys.friendRequests(direction),
    queryFn: () => fetchRequests(direction),
  })
  if (query.isPending) return <ListSkeleton />
  if (query.isError) return <ErrorState onRetry={() => query.refetch()} />
  if (query.data.length === 0) {
    return (
      <EmptyState
        title={direction === 'incoming' ? 'Không có lời mời nào' : 'Bạn chưa gửi lời mời nào'}
      />
    )
  }
  return (
    <Card className="grid gap-4 p-4">
      {query.data.map((r) => (
        <UserRow
          key={r.user.id}
          user={r.user}
          subtitle={formatRelative(r.createdAt)}
          action={
            <FriendActionButton
              userId={r.user.id}
              status={direction === 'incoming' ? 'INCOMING' : 'OUTGOING'}
              size="sm"
            />
          }
        />
      ))}
    </Card>
  )
}

function SuggestionList() {
  const query = useQuery({ queryKey: queryKeys.suggestions, queryFn: fetchSuggestions })
  if (query.isPending) return <ListSkeleton />
  if (query.isError) return <ErrorState onRetry={() => query.refetch()} />
  if (query.data.length === 0) {
    return <EmptyState title="Chưa có gợi ý">Kết bạn thêm để nhận gợi ý từ bạn của bạn.</EmptyState>
  }
  return (
    <Card className="grid gap-4 p-4">
      {query.data.map((s) => (
        <UserRow
          key={s.user.id}
          user={s.user}
          subtitle={`${s.mutualFriends} bạn chung`}
          action={<FriendActionButton userId={s.user.id} status="NONE" size="sm" />}
        />
      ))}
    </Card>
  )
}
