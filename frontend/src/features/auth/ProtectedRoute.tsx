import { Navigate, Outlet, useLocation } from 'react-router'
import { ErrorState, FullPageSpinner } from '@/components/states'
import { useSession } from './session'

export function ProtectedRoute() {
  const session = useSession()
  const location = useLocation()

  if (session.isPending) return <FullPageSpinner />
  if (session.isError) {
    return (
      <div className="mx-auto max-w-md p-4 pt-20">
        <ErrorState message="Không kết nối được máy chủ" onRetry={() => session.refetch()} />
      </div>
    )
  }
  if (!session.data) {
    const next = location.pathname + location.search
    return <Navigate to={`/login?next=${encodeURIComponent(next)}`} replace />
  }
  return <Outlet />
}

/** Trang đăng nhập/đăng ký: đã đăng nhập thì về bảng tin. */
export function PublicOnlyRoute() {
  const session = useSession()
  if (session.isPending) return <FullPageSpinner />
  if (session.data) return <Navigate to="/" replace />
  return <Outlet />
}
