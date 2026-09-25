import type { ReactNode } from 'react'
import { Link } from 'react-router'
import type { UserSummary } from '@/api/types'
import { Avatar } from '@/components/ui/avatar'

export function UserRow({
  user,
  subtitle,
  action,
}: {
  user: UserSummary
  subtitle?: ReactNode
  action?: ReactNode
}) {
  return (
    <div className="flex items-center gap-3">
      <Link to={`/users/${user.id}`} aria-hidden="true" tabIndex={-1}>
        <Avatar name={user.displayName} src={user.avatarUrl} />
      </Link>
      <div className="min-w-0 flex-1">
        <Link to={`/users/${user.id}`} className="block truncate font-medium hover:underline">
          {user.displayName}
        </Link>
        {subtitle && <div className="truncate text-xs text-muted-foreground">{subtitle}</div>}
      </div>
      {action}
    </div>
  )
}
