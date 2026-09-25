import { QueryClient } from '@tanstack/react-query'
import { httpStatus } from './errors'

export function createQueryClient() {
  return new QueryClient({
    defaultOptions: {
      queries: {
        staleTime: 30_000,
        refetchOnWindowFocus: false,
        // Không thử lại lỗi phía client (403/404/...), chỉ thử lại lỗi mạng/5xx
        retry: (failureCount, error) => {
          const status = httpStatus(error)
          return failureCount < 2 && (status === undefined || status >= 500)
        },
      },
    },
  })
}
