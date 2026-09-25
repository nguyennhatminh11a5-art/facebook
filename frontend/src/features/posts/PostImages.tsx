import { useQueryClient } from '@tanstack/react-query'
import { ChevronLeft, ChevronRight } from 'lucide-react'
import { useRef, useState } from 'react'
import type { PostImage } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { invalidatePostQueries } from '@/lib/postCache'
import { cn } from '@/lib/utils'

/** Lưới ảnh dùng thumbnail; bấm mở ảnh đầy đủ. Ảnh lỗi (URL ký hết hạn) → tải lại bài. */
export function PostImages({ images }: { images: PostImage[] }) {
  const queryClient = useQueryClient()
  const [open, setOpen] = useState<number | null>(null)
  const refetched = useRef(false)

  const onError = () => {
    if (refetched.current) return
    refetched.current = true
    invalidatePostQueries(queryClient)
  }

  if (images.length === 0) return null
  const current = open === null ? undefined : images[open]

  return (
    <>
      <div
        className={cn(
          'grid gap-0.5 overflow-hidden',
          images.length === 1 ? 'grid-cols-1' : 'grid-cols-2',
        )}
      >
        {images.map((image, i) => (
          <button
            key={image.thumbUrl}
            type="button"
            className={cn(
              'relative block overflow-hidden bg-muted',
              images.length === 1 ? 'max-h-[32rem]' : 'aspect-square',
              images.length === 3 && i === 0 && 'col-span-2 aspect-[2/1]',
            )}
            onClick={() => setOpen(i)}
            aria-label={`Xem ảnh ${i + 1}/${images.length}`}
          >
            <img
              src={image.thumbUrl}
              alt=""
              loading="lazy"
              width={image.width}
              height={image.height}
              onError={onError}
              className="size-full object-cover"
            />
          </button>
        ))}
      </div>

      <Dialog open={open !== null} onOpenChange={(v) => !v && setOpen(null)}>
        <DialogContent className="max-w-4xl bg-black p-2">
          <DialogTitle className="sr-only">
            Ảnh {open === null ? '' : open + 1}/{images.length}
          </DialogTitle>
          {current && (
            <img
              src={current.url}
              alt=""
              onError={onError}
              className="mx-auto max-h-[80svh] w-auto object-contain"
            />
          )}
          {images.length > 1 && open !== null && (
            <div className="flex justify-between">
              <Button
                variant="secondary"
                size="icon"
                onClick={() => setOpen((open - 1 + images.length) % images.length)}
                aria-label="Ảnh trước"
              >
                <ChevronLeft />
              </Button>
              <Button
                variant="secondary"
                size="icon"
                onClick={() => setOpen((open + 1) % images.length)}
                aria-label="Ảnh sau"
              >
                <ChevronRight />
              </Button>
            </div>
          )}
        </DialogContent>
      </Dialog>
    </>
  )
}
