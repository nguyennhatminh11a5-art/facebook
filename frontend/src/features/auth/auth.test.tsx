import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router'
import { describe, expect, it } from 'vitest'
import { createQueryClient } from '@/lib/queryClient'
import { LoginPage, safeNext } from './LoginPage'
import { registerSchema, RegisterPage } from './RegisterPage'

function renderWithProviders(ui: React.ReactNode) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <MemoryRouter>{ui}</MemoryRouter>
    </QueryClientProvider>,
  )
}

describe('auth forms', () => {
  it('login shows validation errors', async () => {
    renderWithProviders(<LoginPage />)
    await userEvent.click(screen.getByRole('button', { name: 'Đăng nhập' }))
    expect(await screen.findByText('Nhập email')).toBeInTheDocument()
    expect(screen.getByText('Nhập mật khẩu')).toBeInTheDocument()
  })

  it('register rejects mismatched passwords', async () => {
    renderWithProviders(<RegisterPage />)
    await userEvent.type(screen.getByLabelText('Tên hiển thị'), 'An')
    await userEvent.type(screen.getByLabelText('Email'), 'an@example.com')
    await userEvent.type(screen.getByLabelText('Mật khẩu'), 'password123')
    await userEvent.type(screen.getByLabelText('Nhập lại mật khẩu'), 'password124')
    await userEvent.click(screen.getByRole('button', { name: 'Đăng ký' }))
    expect(await screen.findByText('Mật khẩu nhập lại không khớp')).toBeInTheDocument()
  })

  it('register schema counts UTF-8 bytes for password limit', () => {
    const base = { displayName: 'A', email: 'a@b.co' }
    const long = 'ệ'.repeat(25) // 25 ký tự, 75 byte
    expect(
      registerSchema.safeParse({ ...base, password: long, confirmPassword: long }).success,
    ).toBe(false)
    const ok = 'a'.repeat(72)
    expect(registerSchema.safeParse({ ...base, password: ok, confirmPassword: ok }).success).toBe(
      true,
    )
  })

  it('safeNext only allows internal paths', () => {
    expect(safeNext('/users/2')).toBe('/users/2')
    expect(safeNext('//evil.com')).toBe('/')
    expect(safeNext('https://evil.com')).toBe('/')
    expect(safeNext(null)).toBe('/')
  })
})
