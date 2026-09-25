import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Home, LogOut, Search, UserRound, Users } from 'lucide-react'
import type { ReactNode } from 'react'
import { Link, NavLink, Outlet } from 'react-router'
import { logout } from '@/api/auth'
import { ThemeToggle } from '@/components/ThemeToggle'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { clearSession, useCurrentUser } from '@/features/auth/session'
import { cn } from '@/lib/utils'

type NavItem = { to: string; label: string; icon: ReactNode; end?: boolean }

function useNavItems(): NavItem[] {
  const me = useCurrentUser()
  return [
    { to: '/', label: 'Bảng tin', icon: <Home aria-hidden="true" />, end: true },
    { to: '/friends', label: 'Bạn bè', icon: <Users aria-hidden="true" /> },
    { to: '/search', label: 'Tìm kiếm', icon: <Search aria-hidden="true" /> },
    { to: `/users/${me.id}`, label: 'Trang cá nhân', icon: <UserRound aria-hidden="true" /> },
  ]
}

function UserMenu() {
  const me = useCurrentUser()
  const queryClient = useQueryClient()
  const logoutMutation = useMutation({
    mutationFn: logout,
    onSettled: () => clearSession(queryClient),
  })

  return (
    <DropdownMenu>
      <DropdownMenuTrigger asChild>
        <Button variant="ghost" size="icon" className="rounded-full" aria-label="Tài khoản">
          <Avatar name={me.displayName} src={me.avatarUrl} className="size-8" />
        </Button>
      </DropdownMenuTrigger>
      <DropdownMenuContent align="end">
        <DropdownMenuItem asChild>
          <Link to={`/users/${me.id}`}>
            <UserRound /> {me.displayName}
          </Link>
        </DropdownMenuItem>
        <DropdownMenuItem onSelect={() => logoutMutation.mutate()}>
          <LogOut /> Đăng xuất
        </DropdownMenuItem>
      </DropdownMenuContent>
    </DropdownMenu>
  )
}

export function AppLayout() {
  const items = useNavItems()

  return (
    <div className="min-h-svh pb-16 lg:pb-0">
      <header className="sticky top-0 z-40 border-b bg-card/95 backdrop-blur">
        <div className="mx-auto flex h-14 max-w-7xl items-center justify-between gap-2 px-3">
          <Link to="/" className="text-xl font-bold text-primary">
            mini-facebook
          </Link>
          <div className="flex items-center gap-1">
            <ThemeToggle />
            <UserMenu />
          </div>
        </div>
      </header>

      <div className="mx-auto grid max-w-7xl gap-4 px-3 py-4 lg:grid-cols-[220px_minmax(0,1fr)] xl:grid-cols-[240px_minmax(0,1fr)_300px]">
        <nav aria-label="Điều hướng chính" className="hidden lg:block">
          <ul className="sticky top-18 grid gap-1">
            {items.map((item) => (
              <li key={item.to}>
                <NavLink
                  to={item.to}
                  end={item.end}
                  className={({ isActive }) =>
                    cn(
                      'flex items-center gap-3 rounded-lg px-3 py-2 text-sm font-medium hover:bg-accent [&_svg]:size-5',
                      isActive && 'bg-accent text-primary',
                    )
                  }
                >
                  {item.icon}
                  {item.label}
                </NavLink>
              </li>
            ))}
          </ul>
        </nav>

        <main className="mx-auto w-full max-w-2xl min-w-0">
          <Outlet />
        </main>
      </div>

      <nav
        aria-label="Điều hướng"
        className="fixed inset-x-0 bottom-0 z-40 border-t bg-card lg:hidden"
      >
        <ul className="grid grid-cols-4">
          {items.map((item) => (
            <li key={item.to}>
              <NavLink
                to={item.to}
                end={item.end}
                className={({ isActive }) =>
                  cn(
                    'flex h-14 flex-col items-center justify-center gap-0.5 text-[11px] text-muted-foreground [&_svg]:size-5',
                    isActive && 'text-primary',
                  )
                }
              >
                {item.icon}
                <span className="truncate">{item.label}</span>
              </NavLink>
            </li>
          ))}
        </ul>
      </nav>
    </div>
  )
}
