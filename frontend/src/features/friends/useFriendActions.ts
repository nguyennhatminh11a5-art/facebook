import { useMutation, useQueryClient } from '@tanstack/react-query'
import { acceptRequest, deleteRequest, sendRequest, unfriend } from '@/api/friends'
import { invalidatePostQueries } from '@/lib/postCache'
import { queryKeys } from '@/lib/queryKeys'

export type FriendAction = 'send' | 'accept' | 'deleteRequest' | 'unfriend'

async function run(action: FriendAction, userId: number) {
  switch (action) {
    case 'send':
      return sendRequest(userId)
    case 'accept':
      return acceptRequest(userId)
    case 'deleteRequest':
      return deleteRequest(userId)
    case 'unfriend':
      return unfriend(userId)
  }
}

/** Mọi thao tác bạn bè; xong thì làm mới hồ sơ, danh sách bạn, lời mời, gợi ý và bài viết. */
export function useFriendAction(userId: number) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn: (action: FriendAction) => run(action, userId),
    onSettled: () => {
      queryClient.invalidateQueries({ queryKey: queryKeys.profile(userId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.friends })
      queryClient.invalidateQueries({ queryKey: ['friendRequests'] })
      queryClient.invalidateQueries({ queryKey: queryKeys.suggestions })
      invalidatePostQueries(queryClient)
    },
  })
}
