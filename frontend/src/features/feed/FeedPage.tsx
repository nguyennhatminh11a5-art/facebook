import { EmptyState } from '@/components/states'
import { useCurrentUser } from '@/features/auth/session'

export function FeedPage() {
  const me = useCurrentUser()
  return <EmptyState title={`Xin chào, ${me.displayName}!`}>Bảng tin sẽ hiển thị ở đây.</EmptyState>
}
