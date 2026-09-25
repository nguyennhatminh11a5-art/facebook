import { Check, UserCheck, UserMinus, UserPlus, UserX } from 'lucide-react'
import type { FriendStatus } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { errorMessage } from '@/lib/errors'
import { useFriendAction } from './useFriendActions'

type Props = { userId: number; status: FriendStatus; size?: 'sm' | 'default' }

export function FriendActionButton({ userId, status, size = 'default' }: Props) {
  const action = useFriendAction(userId)
  const busy = action.isPending

  const error = action.isError && (
    <p role="alert" className="text-sm text-destructive">
      {errorMessage(action.error)}
    </p>
  )

  switch (status) {
    case 'SELF':
      return null
    case 'NONE':
      return (
        <div className="grid gap-1">
          <Button size={size} disabled={busy} onClick={() => action.mutate('send')}>
            <UserPlus aria-hidden="true" /> Kết bạn
          </Button>
          {error}
        </div>
      )
    case 'OUTGOING':
      return (
        <div className="grid gap-1">
          <Button
            size={size}
            variant="secondary"
            disabled={busy}
            onClick={() => action.mutate('deleteRequest')}
          >
            <UserX aria-hidden="true" /> Huỷ lời mời
          </Button>
          {error}
        </div>
      )
    case 'INCOMING':
      return (
        <div className="grid gap-1">
          <div className="flex gap-2">
            <Button size={size} disabled={busy} onClick={() => action.mutate('accept')}>
              <Check aria-hidden="true" /> Chấp nhận
            </Button>
            <Button
              size={size}
              variant="secondary"
              disabled={busy}
              onClick={() => action.mutate('deleteRequest')}
            >
              Từ chối
            </Button>
          </div>
          {error}
        </div>
      )
    case 'FRIENDS':
      return (
        <div className="grid gap-1">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button size={size} variant="secondary" disabled={busy}>
                <UserCheck aria-hidden="true" /> Bạn bè
              </Button>
            </DropdownMenuTrigger>
            <DropdownMenuContent>
              <DropdownMenuItem
                variant="destructive"
                onSelect={() => {
                  if (window.confirm('Huỷ kết bạn?')) action.mutate('unfriend')
                }}
              >
                <UserMinus /> Huỷ kết bạn
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
          {error}
        </div>
      )
  }
}
