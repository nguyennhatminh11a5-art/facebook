import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { updatePost } from '@/api/posts'
import type { Post } from '@/api/types'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { errorMessage } from '@/lib/errors'
import { updatePostInCaches } from '@/lib/postCache'

type Props = { post: Post; open: boolean; onOpenChange: (open: boolean) => void }

export function EditPostDialog({ post, open, onOpenChange }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogTitle>Chỉnh sửa bài viết</DialogTitle>
        <DialogDescription>Chỉ sửa được nội dung, không sửa được ảnh.</DialogDescription>
        {open && <EditForm post={post} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  )
}

function EditForm({ post, onDone }: { post: Post; onDone: () => void }) {
  const queryClient = useQueryClient()
  const [content, setContent] = useState(post.content ?? '')
  const mutation = useMutation({
    mutationFn: () => updatePost(post.id, content),
    onSuccess: (updated) => {
      updatePostInCaches(queryClient, post.id, () => updated)
      onDone()
    },
  })
  const emptyNotAllowed = content.trim() === '' && post.images.length === 0

  return (
    <form
      className="grid gap-3"
      onSubmit={(e) => {
        e.preventDefault()
        mutation.mutate()
      }}
    >
      <Textarea
        aria-label="Nội dung"
        value={content}
        onChange={(e) => setContent(e.target.value)}
        className="min-h-32"
        maxLength={5000}
      />
      {emptyNotAllowed && (
        <p className="text-sm text-destructive">Bài không có ảnh thì không được để trống.</p>
      )}
      {mutation.isError && (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(mutation.error)}
        </p>
      )}
      <div className="flex justify-end gap-2">
        <Button type="button" variant="outline" onClick={onDone}>
          Huỷ
        </Button>
        <Button type="submit" disabled={emptyNotAllowed || mutation.isPending}>
          Lưu
        </Button>
      </div>
    </form>
  )
}
