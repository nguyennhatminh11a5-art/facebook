import { QueryClientProvider } from '@tanstack/react-query'
import { useState } from 'react'
import { RouterProvider } from 'react-router'
import { setAuthFailureHandler } from '@/api/client'
import { clearSession } from '@/features/auth/session'
import { createQueryClient } from '@/lib/queryClient'
import { router } from '@/routes/router'

export default function App() {
  const [queryClient] = useState(() => {
    const client = createQueryClient()
    // Refresh thất bại → xoá phiên → ProtectedRoute chuyển về trang đăng nhập
    setAuthFailureHandler(() => clearSession(client))
    return client
  })

  return (
    <QueryClientProvider client={queryClient}>
      <RouterProvider router={router} />
    </QueryClientProvider>
  )
}
