import { useMutation, useQueryClient } from '@tanstack/react-query'
import { MoreHorizontal, Pencil, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { deletePost } from '@/api/posts'
import type { Post } from '@/api/types'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Card } from '@/components/ui/card'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { CommentSection } from '@/features/comments/CommentSection'
import { errorMessage } from '@/lib/errors'
import { removePostFromCaches } from '@/lib/postCache'
import { formatFull, formatRelative } from '@/lib/time'
import { EditPostDialog } from './EditPostDialog'
import { PostImages } from './PostImages'
import { ReactionBar } from './ReactionBar'

type Props = { post: Post; defaultShowComments?: boolean; onDeleted?: () => void }

export function PostCard({ post, defaultShowComments = false, onDeleted }: Props) {
  const queryClient = useQueryClient()
  const [showComments, setShowComments] = useState(defaultShowComments)
  const [editing, setEditing] = useState(false)

  const deletion = useMutation({
    mutationFn: () => deletePost(post.id),
    onSuccess: () => {
      removePostFromCaches(queryClient, post.id)
      onDeleted?.()
    },
  })

  const edited = post.updatedAt !== post.createdAt

  return (
    <Card className="overflow-hidden">
      <article aria-labelledby={`post-${post.id}-author`}>
        <header className="flex items-start gap-3 p-4 pb-2">
          <Link to={`/users/${post.author.id}`} aria-hidden="true" tabIndex={-1}>
            <Avatar name={post.author.displayName} src={post.author.avatarUrl} />
          </Link>
          <div className="min-w-0 flex-1">
            <Link
              id={`post-${post.id}-author`}
              to={`/users/${post.author.id}`}
              className="block truncate font-semibold hover:underline"
            >
              {post.author.displayName}
            </Link>
            <Link
              to={`/posts/${post.id}`}
              className="text-xs text-muted-foreground hover:underline"
              title={formatFull(post.createdAt)}
            >
              <time dateTime={post.createdAt}>{formatRelative(post.createdAt)}</time>
              {edited && ' · Đã chỉnh sửa'}
            </Link>
          </div>
          {post.editable && (
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="icon" aria-label="Tuỳ chọn bài viết">
                  <MoreHorizontal />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end">
                <DropdownMenuItem onSelect={() => setEditing(true)}>
                  <Pencil /> Chỉnh sửa
                </DropdownMenuItem>
                <DropdownMenuItem
                  variant="destructive"
                  onSelect={() => {
                    if (window.confirm('Xoá bài viết này?')) deletion.mutate()
                  }}
                >
                  <Trash2 /> Xoá
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          )}
        </header>

        {post.content && (
          <p className="px-4 pb-3 break-words whitespace-pre-wrap">{post.content}</p>
        )}
        <PostImages images={post.images} />

        {deletion.isError && (
          <p role="alert" className="px-4 pt-2 text-sm text-destructive">
            {errorMessage(deletion.error)}
          </p>
        )}

        <div className="px-3 pb-1">
          <ReactionBar post={post} onToggleComments={() => setShowComments((v) => !v)} />
        </div>
        {showComments && <CommentSection post={post} />}
      </article>
      {post.editable && <EditPostDialog post={post} open={editing} onOpenChange={setEditing} />}
    </Card>
  )
}
