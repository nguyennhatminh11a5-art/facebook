import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useNavigate } from 'react-router'
import { z } from 'zod'
import { login, register } from '@/api/auth'
import { FormField } from '@/components/FormField'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { errorMessage } from '@/lib/errors'
import { AuthLayout } from './AuthLayout'
import { setSessionUser } from './session'

const utf8Bytes = (value: string) => new TextEncoder().encode(value).length

export const registerSchema = z
  .object({
    displayName: z.string().trim().min(1, 'Nhập tên hiển thị').max(50, 'Tên tối đa 50 ký tự'),
    email: z.string().trim().min(1, 'Nhập email').email('Email không hợp lệ').max(255),
    password: z
      .string()
      .min(8, 'Mật khẩu tối thiểu 8 ký tự')
      .refine((v) => utf8Bytes(v) <= 72, 'Mật khẩu quá dài (tối đa 72 byte)'),
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: 'Mật khẩu nhập lại không khớp',
  })
type FormValues = z.infer<typeof registerSchema>

export function RegisterPage() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()
  const form = useForm<FormValues>({ resolver: zodResolver(registerSchema) })
  const { errors } = form.formState

  const mutation = useMutation({
    mutationFn: async ({ email, password, displayName }: FormValues) => {
      await register({ email, password, displayName })
      return login({ email, password })
    },
    onSuccess: (data) => {
      setSessionUser(queryClient, data.user)
      navigate('/', { replace: true })
    },
  })

  return (
    <AuthLayout title="Tạo tài khoản">
      <form
        noValidate
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) => mutation.mutate(values))}
      >
        <FormField id="displayName" label="Tên hiển thị" error={errors.displayName?.message}>
          <Input
            id="displayName"
            autoComplete="name"
            aria-invalid={!!errors.displayName}
            {...form.register('displayName')}
          />
        </FormField>
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
            autoComplete="new-password"
            aria-invalid={!!errors.password}
            {...form.register('password')}
          />
        </FormField>
        <FormField
          id="confirmPassword"
          label="Nhập lại mật khẩu"
          error={errors.confirmPassword?.message}
        >
          <Input
            id="confirmPassword"
            type="password"
            autoComplete="new-password"
            aria-invalid={!!errors.confirmPassword}
            {...form.register('confirmPassword')}
          />
        </FormField>
        {mutation.isError && (
          <p role="alert" className="text-sm text-destructive">
            {errorMessage(mutation.error)}
          </p>
        )}
        <Button type="submit" disabled={mutation.isPending}>
          {mutation.isPending ? 'Đang tạo tài khoản…' : 'Đăng ký'}
        </Button>
        <p className="text-center text-sm text-muted-foreground">
          Đã có tài khoản?{' '}
          <Link to="/login" className="font-medium text-primary hover:underline">
            Đăng nhập
          </Link>
        </p>
      </form>
    </AuthLayout>
  )
}
