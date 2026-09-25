import { AlertCircle, Inbox, Loader2, RotateCw } from 'lucide-react'
import type { ReactNode } from 'react'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { cn } from '@/lib/utils'

export function ErrorState({
  message = 'Không tải được dữ liệu',
  onRetry,
  className,
}: {
  message?: string
  onRetry?: () => void
  className?: string
}) {
  return (
    <Card
      role="alert"
      className={cn('flex flex-col items-center gap-3 p-6 text-center', className)}
    >
      <AlertCircle className="size-8 text-destructive" aria-hidden="true" />
      <p className="text-sm">{message}</p>
      {onRetry && (
        <Button variant="outline" size="sm" onClick={onRetry}>
          <RotateCw aria-hidden="true" /> Thử lại
        </Button>
      )}
    </Card>
  )
}

export function EmptyState({
  title,
  children,
  className,
}: {
  title: string
  children?: ReactNode
  className?: string
}) {
  return (
    <Card className={cn('flex flex-col items-center gap-2 p-8 text-center', className)}>
      <Inbox className="size-8 text-muted-foreground" aria-hidden="true" />
      <p className="font-medium">{title}</p>
      {children && <div className="text-sm text-muted-foreground">{children}</div>}
    </Card>
  )
}

export function FullPageSpinner() {
  return (
    <div className="grid min-h-svh place-items-center" role="status" aria-label="Đang tải">
      <Loader2 className="size-8 animate-spin text-primary" aria-hidden="true" />
    </div>
  )
}
