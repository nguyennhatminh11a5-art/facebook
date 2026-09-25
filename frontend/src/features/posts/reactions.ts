import type { Post, ReactionType } from '@/api/types'

export const REACTIONS: Record<ReactionType, { emoji: string; label: string; color: string }> = {
  LIKE: { emoji: '👍', label: 'Thích', color: 'text-primary' },
  LOVE: { emoji: '❤️', label: 'Yêu thích', color: 'text-red-500' },
  HAHA: { emoji: '😆', label: 'Haha', color: 'text-amber-500' },
  WOW: { emoji: '😮', label: 'Wow', color: 'text-amber-500' },
  SAD: { emoji: '😢', label: 'Buồn', color: 'text-amber-500' },
  ANGRY: { emoji: '😡', label: 'Phẫn nộ', color: 'text-orange-600' },
}

/** Cập nhật lạc quan: thêm mới +1, đổi loại giữ nguyên, bỏ -1. */
export function applyReaction(post: Post, next: ReactionType | null): Post {
  const had = post.myReaction !== null
  const has = next !== null
  const delta = had === has ? 0 : has ? 1 : -1
  return { ...post, myReaction: next, reactionCount: Math.max(0, post.reactionCount + delta) }
}
