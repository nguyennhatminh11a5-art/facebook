import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useSearchParams } from 'react-router'
import { z } from 'zod'
import { login } from '@/api/auth'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { errorMessage } from '@/lib/errors'
import { AuthLayout } from './AuthLayout'
import { setSessionUser } from './session'

const schema = z.object({
  email: z.string().trim().min(1, 'Nhập email').email('Email không hợp lệ'),
  password: z.string().min(1, 'Nhập mật khẩu'),
})
type FormValues = z.infer<typeof schema>

/** Chỉ cho phép chuyển tiếp tới đường dẫn nội bộ (chống open redirect). */
export function safeNext(next: string | null): string {
  return next && next.startsWith('/') && !next.startsWith('//') ? next : '/'
}

export function LoginPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const [params] = useSearchParams()
  const form = useForm<FormValues>({ resolver: zodResolver(schema) })
  const { errors } = form.formState

  const mutation = useMutation({
    mutationFn: login,
    onSuccess: (data) => {
      setSessionUser(queryClient, data.user)
      navigate(safeNext(params.get('next')), { replace: true })
    },
  })

  return (
    <AuthLayout title="Đăng nhập">
      <form
        noValidate
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      >
        <FormField id="email" label="Email" error={errors.email?.message}>
          <Input
            id="email"
            type="email"
            autoComplete="email"
            aria-invalid={!!errors.email}
            {...form.register('email')}
          />
        </FormField>
        <FormField id="password" label="Mật khẩu" error={errors.password?.message}>
          <Input
            id="password"
            type="password"
            autoComplete="current-password"
            aria-invalid={!!errors.password}
            {...form.register('password')}
          />
        </FormField>
        {mutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {errorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Đang đăng nhập…' : 'Đăng nhập'}
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          Chưa có tài khoản?{' '}
          <Link to="/register" className="font-medium text-primary hover:underline">
            Đăng ký
          </Link>
        </p>
      </form>
    </AuthLayout>
  )
}
