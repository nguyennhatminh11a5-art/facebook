/** Khoá TanStack Query dùng chung. */
export const queryKeys = {
  feed: ['feed'] as const,
  userPosts: (userId: number) => ['userPosts', userId] as const,
  post: (id: number) => ['post', id] as const,
  comments: (postId: number) => ['comments', postId] as const,
  profile: (id: number) => ['profile', id] as const,
  search: (q: string) => ['search', q] as const,
  friends: ['friends'] as const,
  friendRequests: (direction: 'incoming' | 'outgoing') => ['friendRequests', direction] as const,
  suggestions: ['suggestions'] as const,
}

/** Các query chứa bài viết (để cập nhật lạc quan / tải lại khi URL ảnh hết hạn). */
export const POST_QUERY_ROOTS = ['feed', 'userPosts', 'post'] as const
