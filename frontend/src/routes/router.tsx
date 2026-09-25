import { createBrowserRouter } from 'react-router'
import { AppLayout } from '@/components/AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/ProtectedRoute'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { FeedPage } from '@/features/feed/FeedPage'
import { NotFoundPage } from './NotFoundPage'

export const router = createBrowserRouter([
  {
    element: <PublicOnlyRoute />,
    children: [
      { path: '/login', element: <LoginPage /> },
      { path: '/register', element: <RegisterPage /> },
    ],
  },
  {
    element: <ProtectedRoute />,
    children: [
      {
        element: <AppLayout />,
        children: [
          { index: true, element: <FeedPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
