export type Me = {
  id: number
  email: string
  displayName: string
  bio: string | null
  avatarUrl: string | null
}

export type AuthResponse = {
  accessToken: string
  user: Me
}

export type UserSummary = {
  id: number
  displayName: string
  avatarUrl: string | null
}

export type FriendStatus = 'NONE' | 'OUTGOING' | 'INCOMING' | 'FRIENDS' | 'SELF'

export type Profile = {
  id: number
  displayName: string
  bio: string | null
  avatarUrl: string | null
  friendStatus: FriendStatus
}

export type Page<T> = {
  items: T[]
  nextCursor: string | null
}

export const REACTION_TYPES = ['LIKE', 'LOVE', 'HAHA', 'WOW', 'SAD', 'ANGRY'] as const
export type ReactionType = (typeof REACTION_TYPES)[number]

export type PostImage = {
  url: string
  thumbUrl: string
  width: number
  height: number
}

export type Post = {
  id: number
  author: UserSummary
  content: string | null
  images: PostImage[]
  reactionCount: number
  commentCount: number
  myReaction: ReactionType | null
  createdAt: string
  updatedAt: string
  editable: boolean
}

export type Comment = {
  id: number
  postId: number
  author: UserSummary
  content: string
  createdAt: string
  deletable: boolean
}

export type UserPostsPage = Page<Post> & { friendStatus: FriendStatus }

export type FriendRequest = {
  user: UserSummary
  createdAt: string
}

export type FriendSuggestion = {
  user: UserSummary
  mutualFriends: number
}
