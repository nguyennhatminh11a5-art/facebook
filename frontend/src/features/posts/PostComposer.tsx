import { useMutation, useQueryClient } from '@tanstack/react-query'
import { ImagePlus, X } from 'lucide-react'
import { useEffect, useId, useMemo, useRef, useState } from 'react'
import { createPost } from '@/api/posts'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import { Textarea } from '@/components/ui/textarea'
import { useCurrentUser } from '@/features/auth/session'
import { errorMessage } from '@/lib/errors'
import { queryKeys } from '@/lib/queryKeys'
import { ACCEPTED_IMAGE_TYPES, MAX_IMAGES, validateImages } from './imageValidation'

const MAX_CONTENT = 5000

export function PostComposer() {
  const me = useCurrentUser()
  const queryClient = useQueryClient()
  const inputId = useId()
  const fileInput = useRef<HTMLInputElement>(null)
  const [content, setContent] = useState('')
  const [images, setImages] = useState<File[]>([])
  const [localError, setLocalError] = useState<string | null>(null)

  const previews = useMemo(() => images.map((file) => URL.createObjectURL(file)), [images])
  useEffect(() => () => previews.forEach((url) => URL.revokeObjectURL(url)), [previews])

  const mutation = useMutation({
    mutationFn: createPost,
    onSuccess: () => {
      setContent('')
      setImages([])
      queryClient.invalidateQueries({ queryKey: queryKeys.feed })
      queryClient.invalidateQueries({ queryKey: queryKeys.userPosts(me.id) })
    },
  })

  const addFiles = (files: FileList | null) => {
    if (!files) return
    const added = Array.from(files)
    const error = validateImages(images, added)
    setLocalError(error)
    if (!error) setImages((prev) => [...prev, ...added])
    if (fileInput.current) fileInput.current.value = ''
  }

  const trimmed = content.trim()
  const tooLong = content.length > MAX_CONTENT
  const canSubmit = (trimmed.length > 0 || images.length > 0) && !tooLong && !mutation.isPending

  return (
    <Card className="p-4">
      <form
        onSubmit={(e) => {
          e.preventDefault()
          if (canSubmit) mutation.mutate({ content, images })
        }}
        className="grid gap-3"
      >
        <div className="flex gap-3">
          <Avatar name={me.displayName} src={me.avatarUrl} />
          <label htmlFor={inputId} className="sr-only">
            Nội dung bài viết
          </label>
          <Textarea
            id={inputId}
            value={content}
            onChange={(e) => setContent(e.target.value)}
            placeholder={`${me.displayName} ơi, bạn đang nghĩ gì?`}
            className="min-h-20 resize-y"
            aria-invalid={tooLong}
          />
        </div>

        {previews.length > 0 && (
          <ul className="grid grid-cols-2 gap-2 sm:grid-cols-4">
            {previews.map((url, i) => (
              <li key={url} className="relative aspect-square overflow-hidden rounded-md border">
                <img src={url} alt={`Ảnh ${i + 1}`} className="size-full object-cover" />
                <Button
                  type="button"
                  size="icon"
                  variant="secondary"
                  className="absolute top-1 right-1 size-7 rounded-full"
                  onClick={() => setImages((prev) => prev.filter((_, idx) => idx !== i))}
                  aria-label={`Bỏ ảnh ${i + 1}`}
                >
                  <X />
                </Button>
              </li>
            ))}
          </ul>
        )}

        {(localError || tooLong || mutation.isError) && (
          <p role="alert" className="text-sm text-destructive">
            {localError ??
              (tooLong ? `Nội dung tối đa ${MAX_CONTENT} ký tự` : errorMessage(mutation.error))}
          </p>
        )}

        <div className="flex items-center justify-between gap-2">
          <input
            ref={fileInput}
            type="file"
            accept={ACCEPTED_IMAGE_TYPES.join(',')}
            multiple
            hidden
            onChange={(e) => addFiles(e.target.files)}
            data-testid="image-input"
          />
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={() => fileInput.current?.click()}
            disabled={images.length >= MAX_IMAGES}
          >
            <ImagePlus className="text-green-600" aria-hidden="true" /> Ảnh ({images.length}/
            {MAX_IMAGES})
          </Button>
          <Button type="submit" disabled={!canSubmit}>
            {mutation.isPending ? 'Đang đăng…' : 'Đăng'}
          </Button>
        </div>
      </form>
    </Card>
  )
}
