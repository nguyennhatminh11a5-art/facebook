import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router'
import { fetchRequests, fetchSuggestions } from '@/api/friends'
import { UserRow } from '@/components/UserRow'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { queryKeys } from '@/lib/queryKeys'
import { FriendActionButton } from './FriendActionButton'

const LIMIT = 5

function ListSkeleton() {
  return (
    <div className="grid gap-3" role="status" aria-label="Đang tải">
      <Skeleton className="h-10" />
      <Skeleton className="h-10" />
    </div>
  )
}

/** Cột phải (desktop): lời mời kết bạn + gợi ý. */
export function FriendsSidebar() {
  const requests = useQuery({
    queryKey: queryKeys.friendRequests('incoming'),
    queryFn: () => fetchRequests('incoming'),
  })
  const suggestions = useQuery({ queryKey: queryKeys.suggestions, queryFn: fetchSuggestions })

  return (
    <div className="grid gap-4">
      <Card>
        <CardHeader>
          <CardTitle className="text-base">Lời mời kết bạn</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3">
          {requests.isPending && <ListSkeleton />}
          {requests.isError && (
            <button
              type="button"
              className="text-left text-sm text-destructive"
              onClick={() => requests.refetch()}
            >
              Không tải được. Thử lại
            </button>
          )}
          {requests.data?.length === 0 && (
            <p className="text-sm text-muted-foreground">Không có lời mời nào.</p>
          )}
          {requests.data?.slice(0, LIMIT).map((r) => (
            <div key={r.user.id} className="grid gap-2">
              <UserRow user={r.user} />
              <FriendActionButton userId={r.user.id} status="INCOMING" size="sm" />
            </div>
          ))}
          {(requests.data?.length ?? 0) > LIMIT && (
            <Link to="/friends?tab=incoming" className="text-sm text-primary hover:underline">
              Xem tất cả
            </Link>
          )}
        </CardContent>
      </Card>

      <Card>
        <CardHeader>
          <CardTitle className="text-base">Những người bạn có thể biết</CardTitle>
        </CardHeader>
        <CardContent className="grid gap-3">
          {suggestions.isPending && <ListSkeleton />}
          {suggestions.isError && (
            <button
              type="button"
              className="text-left text-sm text-destructive"
              onClick={() => suggestions.refetch()}
            >
              Không tải được. Thử lại
            </button>
          )}
          {suggestions.data?.length === 0 && (
            <p className="text-sm text-muted-foreground">
              Chưa có gợi ý. Kết bạn thêm để nhận gợi ý từ bạn của bạn.
            </p>
          )}
          {suggestions.data?.slice(0, LIMIT).map((s) => (
            <UserRow
              key={s.user.id}
              user={s.user}
              subtitle={`${s.mutualFriends} bạn chung`}
              action={<FriendActionButton userId={s.user.id} status="NONE" size="sm" />}
            />
          ))}
        </CardContent>
      </Card>
    </div>
  )
}
