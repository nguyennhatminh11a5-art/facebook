import { useQuery, useQueryClient, type QueryClient } from '@tanstack/react-query'
import { refreshSession } from '@/api/client'
import type { Me } from '@/api/types'
import { httpStatus } from '@/lib/errors'

export const sessionKey = ['session'] as const

/** Khôi phục phiên khi tải trang: cookie refresh → access token mới. 401 → chưa đăng nhập. */
async function restoreSession(): Promise<Me | null> {
  try {
    const res = await refreshSession()
    return res.user
  } catch (error) {
    if (httpStatus(error) === 401) return null
    throw error
  }
}

export function useSession() {
  return useQuery({
    queryKey: sessionKey,
    queryFn: restoreSession,
    staleTime: Infinity,
    gcTime: Infinity,
    retry: false,
  })
}

/** Người dùng hiện tại — chỉ dùng bên trong ProtectedRoute. */
export function useCurrentUser(): Me {
  const { data } = useSession()
  if (!data) throw new Error('useCurrentUser dùng ngoài ProtectedRoute')
  return data
}

export function setSessionUser(queryClient: QueryClient, user: Me | null) {
  queryClient.setQueryData(sessionKey, user)
}

/** Xoá toàn bộ dữ liệu của phiên cũ, giữ lại khoá session. */
export function clearSession(queryClient: QueryClient) {
  queryClient.removeQueries({ predicate: (q) => q.queryKey[0] !== sessionKey[0] })
  setSessionUser(queryClient, null)
}

export function useSetMe() {
  const queryClient = useQueryClient()
  return (user: Me) => setSessionUser(queryClient, user)
}
