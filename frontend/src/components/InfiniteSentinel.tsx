import { Loader2 } from 'lucide-react'
import { useEffect, useRef } from 'react'
import { Button } from '@/components/ui/button'

type Props = {
  hasNextPage: boolean
  isFetchingNextPage: boolean
  fetchNextPage: () => unknown
  label?: string
}

/** Tự tải trang sau khi cuộn tới cuối; có nút dự phòng cho bàn phím/trình đọc màn hình. */
export function InfiniteSentinel({
  hasNextPage,
  isFetchingNextPage,
  fetchNextPage,
  label = 'Tải thêm',
}: Props) {
  const ref = useRef<HTMLDivElement>(null)

  useEffect(() => {
    const el = ref.current
    if (!el || !hasNextPage || typeof IntersectionObserver === 'undefined') return
    const observer = new IntersectionObserver(
      (entries) => {
        if (entries.some((e) => e.isIntersecting) && !isFetchingNextPage) fetchNextPage()
      },
      { rootMargin: '400px' },
    )
    observer.observe(el)
    return () => observer.disconnect()
  }, [hasNextPage, isFetchingNextPage, fetchNextPage])

  if (!hasNextPage) return null
  return (
    <div ref={ref} className="flex justify-center py-4">
      {isFetchingNextPage ? (
        <Loader2 className="size-6 animate-spin text-muted-foreground" aria-label="Đang tải" />
      ) : (
        <Button variant="ghost" size="sm" onClick={() => fetchNextPage()}>
          {label}
        </Button>
      )}
    </div>
  )
}
