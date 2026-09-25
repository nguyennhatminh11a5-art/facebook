import { useInfiniteQuery, useQuery } from '@tanstack/react-query'
import { Lock, Pencil } from 'lucide-react'
import { useState } from 'react'
import { useParams } from 'react-router'
import { fetchUserPosts } from '@/api/posts'
import type { FriendStatus, Profile } from '@/api/types'
import { fetchProfile } from '@/api/users'
import { EmptyState, ErrorState } from '@/components/states'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/features/auth/session'
import { FriendActionButton } from '@/features/friends/FriendActionButton'
import { PostComposer } from '@/features/posts/PostComposer'
import { PostList } from '@/features/posts/PostList'
import { httpStatus } from '@/lib/errors'
import { queryKeys } from '@/lib/queryKeys'
import { NotFoundPage } from '@/routes/NotFoundPage'
import { EditProfileDialog } from './EditProfileDialog'

const HIDDEN_REASON: Partial<Record<FriendStatus, string>> = {
  NONE: 'Hãy kết bạn để xem bài viết của người này.',
  OUTGOING: 'Bạn đã gửi lời mời kết bạn. Bài viết sẽ hiện khi lời mời được chấp nhận.',
  INCOMING: 'Người này đã gửi lời mời kết bạn cho bạn. Chấp nhận để xem bài viết.',
}

export function ProfilePage() {
  const id = Number(useParams().id)
  const valid = Number.isInteger(id) && id > 0
  const profile = useQuery({
    queryKey: queryKeys.profile(id),
    queryFn: () => fetchProfile(id),
    enabled: valid,
  })

  if (!valid) return <NotFoundPage />
  if (profile.isPending) return <ProfileHeaderSkeleton />
  if (profile.isError) {
    if (httpStatus(profile.error) === 404) return <NotFoundPage />
    return <ErrorState onRetry={() => profile.refetch()} />
  }
  return (
    <div className="grid gap-4">
      <ProfileHeader profile={profile.data} />
      <ProfilePosts profile={profile.data} />
    </div>
  )
}

function ProfileHeader({ profile }: { profile: Profile }) {
  const me = useCurrentUser()
  const [editing, setEditing] = useState(false)
  const isMe = profile.friendStatus === 'SELF'
  // Hồ sơ của chính mình lấy từ phiên để thấy ngay thay đổi sau khi sửa
  const shown = isMe ? { ...profile, ...me } : profile

  return (
    <Card className="flex flex-col items-center gap-3 p-6 text-center sm:flex-row sm:text-left">
      <Avatar name={shown.displayName} src={shown.avatarUrl} className="size-24 text-3xl" />
      <div className="min-w-0 flex-1">
        <h1 className="text-2xl font-bold break-words">{shown.displayName}</h1>
        {shown.bio && (
          <p className="mt-1 break-words whitespace-pre-wrap text-muted-foreground">{shown.bio}</p>
        )}
      </div>
      {isMe ? (
        <>
          <Button variant="secondary" onClick={() => setEditing(true)}>
            <Pencil aria-hidden="true" /> Chỉnh sửa
          </Button>
          <EditProfileDialog me={me} open={editing} onOpenChange={setEditing} />
        </>
      ) : (
        <FriendActionButton userId={profile.id} status={profile.friendStatus} />
      )}
    </Card>
  )
}

function ProfilePosts({ profile }: { profile: Profile }) {
  const query = useInfiniteQuery({
    queryKey: queryKeys.userPosts(profile.id),
    queryFn: ({ pageParam }) => fetchUserPosts(profile.id, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })
  const status = query.data?.pages[0]?.friendStatus ?? profile.friendStatus
  const hidden = HIDDEN_REASON[status]

  if (query.isSuccess && hidden) {
    return (
      <Card className="flex flex-col items-center gap-2 p-8 text-center">
        <Lock className="size-8 text-muted-foreground" aria-hidden="true" />
        <p className="font-medium">Bài viết chỉ hiển thị với bạn bè</p>
        <p className="text-sm text-muted-foreground">{hidden}</p>
      </Card>
    )
  }

  return (
    <div className="grid gap-4">
      {status === 'SELF' && <PostComposer />}
      <PostList
        query={query}
        empty={
          <EmptyState title="Chưa có bài viết nào">
            {status === 'SELF' ? 'Hãy chia sẻ điều gì đó!' : undefined}
          </EmptyState>
        }
      />
    </div>
  )
}

function ProfileHeaderSkeleton() {
  return (
    <Card className="flex items-center gap-4 p-6" role="status" aria-label="Đang tải hồ sơ">
      <Skeleton className="size-24 rounded-full" />
      <div className="grid flex-1 gap-2">
        <Skeleton className="h-6 w-48" />
        <Skeleton className="h-4 w-64" />
      </div>
    </Card>
  )
}
