import {
  useInfiniteQuery,
  useMutation,
  useQueryClient,
  type InfiniteData,
} from '@tanstack/react-query'
import { SendHorizontal, Trash2 } from 'lucide-react'
import { useState } from 'react'
import { Link } from 'react-router'
import { addComment, deleteComment, fetchComments } from '@/api/comments'
import type { Comment, Page, Post } from '@/api/types'
import { ErrorState } from '@/components/states'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Skeleton } from '@/components/ui/skeleton'
import { useCurrentUser } from '@/features/auth/session'
import { errorMessage } from '@/lib/errors'
import { restoreSnapshot, snapshotPostCaches, updatePostInCaches } from '@/lib/postCache'
import { queryKeys } from '@/lib/queryKeys'
import { formatFull, formatRelative } from '@/lib/time'

type CommentPages = InfiniteData<Page<Comment>, string | null>

const MAX_COMMENT = 1000

export function CommentSection({ post }: { post: Post }) {
  const me = useCurrentUser()
  const queryClient = useQueryClient()
  const key = queryKeys.comments(post.id)
  const [content, setContent] = useState('')

  const query = useInfiniteQuery({
    queryKey: key,
    queryFn: ({ pageParam }) => fetchComments(post.id, pageParam),
    initialPageParam: null as string | null,
    getNextPageParam: (last) => last.nextCursor,
  })

  const changeCount = (delta: number) =>
    updatePostInCaches(queryClient, post.id, (p) => ({
      ...p,
      commentCount: Math.max(0, p.commentCount + delta),
    }))

  const add = useMutation({
    mutationFn: (text: string) => addComment(post.id, text),
    onMutate: async (text) => {
      await queryClient.cancelQueries({ queryKey: key })
      const previousComments = queryClient.getQueryData<CommentPages>(key)
      const postSnapshot = await snapshotPostCaches(queryClient)
      const optimistic: Comment = {
        id: -Date.now(),
        postId: post.id,
        author: { id: me.id, displayName: me.displayName, avatarUrl: me.avatarUrl },
        content: text.trim(),
        createdAt: new Date().toISOString(),
        deletable: true,
      }
      queryClient.setQueryData<CommentPages>(key, (data) => appendComment(data, optimistic))
      changeCount(1)
      setContent('')
      return { previousComments, postSnapshot, optimisticId: optimistic.id, text }
    },
    onError: (_e, _text, context) => {
      if (!context) return
      queryClient.setQueryData(key, context.previousComments)
      restoreSnapshot(queryClient, context.postSnapshot)
      setContent(context.text)
    },
    onSuccess: (saved, _text, context) => {
      queryClient.setQueryData<CommentPages>(key, (data) =>
        mapComments(data, (c) => (c.id === context?.optimisticId ? saved : c)),
      )
    },
  })

  const remove = useMutation({
    mutationFn: (commentId: number) => deleteComment(commentId),
    onMutate: async (commentId) => {
      await queryClient.cancelQueries({ queryKey: key })
      const previousComments = queryClient.getQueryData<CommentPages>(key)
      const postSnapshot = await snapshotPostCaches(queryClient)
      queryClient.setQueryData<CommentPages>(key, (data) =>
        filterComments(data, (c) => c.id !== commentId),
      )
      changeCount(-1)
      return { previousComments, postSnapshot }
    },
    onError: (_e, _id, context) => {
      if (!context) return
      queryClient.setQueryData(key, context.previousComments)
      restoreSnapshot(queryClient, context.postSnapshot)
    },
  })

  const comments = query.data?.pages.flatMap((p) => p.items) ?? []
  const trimmed = content.trim()

  return (
    <section aria-label="Bình luận" className="grid gap-3 border-t px-4 py-3">
      {query.isPending && (
        <div className="grid gap-2" role="status" aria-label="Đang tải bình luận">
          <Skeleton className="h-10 w-3/4" />
          <Skeleton className="h-10 w-1/2" />
        </div>
      )}
      {query.isError && (
        <ErrorState message="Không tải được bình luận" onRetry={() => query.refetch()} />
      )}
      {query.isSuccess && comments.length === 0 && (
        <p className="text-sm text-muted-foreground">Chưa có bình luận. Hãy là người đầu tiên!</p>
      )}

      {comments.length > 0 && (
        <ul className="grid gap-2">
          {comments.map((c) => (
            <li key={c.id} className="group flex gap-2">
              <Link to={`/users/${c.author.id}`} aria-hidden="true" tabIndex={-1}>
                <Avatar name={c.author.displayName} src={c.author.avatarUrl} className="size-8" />
              </Link>
              <div className="min-w-0">
                <div className="rounded-2xl bg-muted px-3 py-2">
                  <Link
                    to={`/users/${c.author.id}`}
                    className="text-sm font-semibold hover:underline"
                  >
                    {c.author.displayName}
                  </Link>
                  <p className="text-sm break-words whitespace-pre-wrap">{c.content}</p>
                </div>
                <div className="flex items-center gap-2 px-2 text-xs text-muted-foreground">
                  <time dateTime={c.createdAt} title={formatFull(c.createdAt)}>
                    {c.id < 0 ? 'Đang gửi…' : formatRelative(c.createdAt)}
                  </time>
                  {c.deletable && c.id > 0 && (
                    <button
                      type="button"
                      className="inline-flex items-center gap-1 hover:text-destructive"
                      onClick={() => {
                        if (window.confirm('Xoá bình luận này?')) remove.mutate(c.id)
                      }}
                    >
                      <Trash2 className="size-3" aria-hidden="true" /> Xoá
                    </button>
                  )}
                </div>
              </div>
            </li>
          ))}
        </ul>
      )}

      {query.hasNextPage && (
        <Button
          variant="link"
          size="sm"
          className="justify-self-start px-0"
          disabled={query.isFetchingNextPage}
          onClick={() => query.fetchNextPage()}
        >
          Xem thêm bình luận
        </Button>
      )}

      {(add.isError || remove.isError) && (
        <p role="alert" className="text-sm text-destructive">
          {errorMessage(add.error ?? remove.error)}
        </p>
      )}

      <form
        className="flex items-center gap-2"
        onSubmit={(e) => {
          e.preventDefault()
          if (trimmed && content.length <= MAX_COMMENT) add.mutate(content)
        }}
      >
        <Avatar name={me.displayName} src={me.avatarUrl} className="size-8" />
        <Input
          aria-label="Viết bình luận"
          placeholder="Viết bình luận…"
          value={content}
          maxLength={MAX_COMMENT}
          onChange={(e) => setContent(e.target.value)}
          className="rounded-full"
        />
        <Button
          type="submit"
          size="icon"
          variant="ghost"
          disabled={!trimmed}
          aria-label="Gửi bình luận"
        >
          <SendHorizontal />
        </Button>
      </form>
    </section>
  )
}

function appendComment(data: CommentPages | undefined, comment: Comment): CommentPages | undefined {
  if (!data || data.pages.length === 0) return data
  const pages = [...data.pages]
  const lastIndex = pages.length - 1
  const last = pages[lastIndex]
  // Chỉ thêm khi đang ở trang cuối (bình luận mới nằm cuối danh sách cũ → mới)
  if (!last || last.nextCursor !== null) return data
  pages[lastIndex] = { ...last, items: [...last.items, comment] }
  return { ...data, pages }
}

function mapComments(data: CommentPages | undefined, fn: (c: Comment) => Comment) {
  if (!data) return data
  return { ...data, pages: data.pages.map((p) => ({ ...p, items: p.items.map(fn) })) }
}

function filterComments(data: CommentPages | undefined, keep: (c: Comment) => boolean) {
  if (!data) return data
  return { ...data, pages: data.pages.map((p) => ({ ...p, items: p.items.filter(keep) })) }
}
