import { useState } from 'react'
import { cn } from '@/lib/utils'

type Props = {
  name: string
  src?: string | null
  className?: string
}

/** Ảnh đại diện, lỗi hoặc chưa có ảnh → chữ cái đầu. */
export function Avatar({ name, src, className }: Props) {
  const [failedSrc, setFailedSrc] = useState<string | null>(null)
  const initials = name
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part.charAt(0).toUpperCase())
    .join('')

  return (
    <span
      className={cn(
        'relative inline-flex size-10 shrink-0 items-center justify-center overflow-hidden rounded-full bg-primary/15 text-sm font-semibold text-primary',
        className,
      )}
    >
      {src && failedSrc !== src ? (
        <img
          src={src}
          alt={name}
          loading="lazy"
          className="size-full object-cover"
          onError={() => setFailedSrc(src)}
        />
      ) : (
        <span aria-hidden="true">{initials || '?'}</span>
      )}
    </span>
  )
}
