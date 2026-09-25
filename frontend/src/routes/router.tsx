import { createBrowserRouter } from 'react-router'
import { AppLayout } from '@/components/AppLayout'
import { LoginPage } from '@/features/auth/LoginPage'
import { ProtectedRoute, PublicOnlyRoute } from '@/features/auth/ProtectedRoute'
import { RegisterPage } from '@/features/auth/RegisterPage'
import { FeedPage } from '@/features/feed/FeedPage'
import { FriendsPage } from '@/features/friends/FriendsPage'
import { PostPage } from '@/features/posts/PostPage'
import { ProfilePage } from '@/features/profile/ProfilePage'
import { SearchPage } from '@/features/search/SearchPage'
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
          { path: 'posts/:id', element: <PostPage /> },
          { path: 'users/:id', element: <ProfilePage /> },
          { path: 'friends', element: <FriendsPage /> },
          { path: 'search', element: <SearchPage /> },
          { path: '*', element: <NotFoundPage /> },
        ],
      },
    ],
  },
])
