import { useMutation, useQueryClient } from '@tanstack/react-query'
import { MessageCircle, SmilePlus, ThumbsUp } from 'lucide-react'
import { react, unreact } from '@/api/posts'
import { REACTION_TYPES, type Post, type ReactionType } from '@/api/types'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { restoreSnapshot, snapshotPostCaches, updatePostInCaches } from '@/lib/postCache'
import { cn } from '@/lib/utils'
import { applyReaction, REACTIONS } from './reactions'

type Props = {
  post: Post
  onToggleComments: () => void
}

export function ReactionBar({ post, onToggleComments }: Props) {
  const queryClient = useQueryClient()

  const mutation = useMutation({
    mutationFn: (next: ReactionType | null) => (next ? react(post.id, next) : unreact(post.id)),
    onMutate: async (next) => {
      const snapshot = await snapshotPostCaches(queryClient)
      updatePostInCaches(queryClient, post.id, (p) => applyReaction(p, next))
      return { snapshot }
    },
    onError: (_error, _next, context) => {
      if (context) restoreSnapshot(queryClient, context.snapshot)
    },
    onSuccess: (summary) => {
      updatePostInCaches(queryClient, post.id, (p) => ({ ...p, ...summary }))
    },
  })

  const mine = post.myReaction ? REACTIONS[post.myReaction] : null

  return (
    <div className="border-t pt-1">
      <div className="flex items-center justify-between px-1 py-1 text-sm text-muted-foreground">
        <span>{post.reactionCount > 0 ? `${post.reactionCount} lượt bày tỏ cảm xúc` : ''}</span>
        <button type="button" className="hover:underline" onClick={onToggleComments}>
          {post.commentCount > 0 ? `${post.commentCount} bình luận` : ''}
        </button>
      </div>
      <div className="grid grid-cols-[1fr_auto_1fr] gap-1">
        <Button
          variant="ghost"
          className={cn('font-semibold', mine?.color)}
          aria-pressed={post.myReaction !== null}
          onClick={() => mutation.mutate(post.myReaction ? null : 'LIKE')}
        >
          {mine ? <span aria-hidden="true">{mine.emoji}</span> : <ThumbsUp aria-hidden="true" />}
          {mine?.label ?? 'Thích'}
        </Button>
        <DropdownMenu>
          <DropdownMenuTrigger asChild>
            <Button variant="ghost" size="icon" aria-label="Chọn cảm xúc">
              <SmilePlus aria-hidden="true" />
            </Button>
          </DropdownMenuTrigger>
          <DropdownMenuContent className="flex min-w-0 gap-1 rounded-full px-2">
            {REACTION_TYPES.map((type) => (
              <DropdownMenuItem
                key={type}
                className={cn(
                  'rounded-full text-2xl transition-transform hover:scale-125 focus:scale-125',
                  post.myReaction === type && 'bg-accent',
                )}
                onSelect={() => mutation.mutate(type)}
                aria-label={REACTIONS[type].label}
              >
                <span aria-hidden="true">{REACTIONS[type].emoji}</span>
              </DropdownMenuItem>
            ))}
          </DropdownMenuContent>
        </DropdownMenu>
        <Button variant="ghost" onClick={onToggleComments}>
          <MessageCircle aria-hidden="true" /> Bình luận
        </Button>
      </div>
    </div>
  )
}
