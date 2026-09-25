import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Camera } from 'lucide-react'
import { useRef, useState } from 'react'
import { useForm } from 'react-hook-form'
import { z } from 'zod'
import type { Me } from '@/api/types'
import { updateMe, uploadAvatar } from '@/api/users'
import { FormField } from '@/components/FormField'
import { Avatar } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Dialog, DialogContent, DialogDescription, DialogTitle } from '@/components/ui/dialog'
import { Input } from '@/components/ui/input'
import { Textarea } from '@/components/ui/textarea'
import { setSessionUser } from '@/features/auth/session'
import { validateImages } from '@/features/posts/imageValidation'
import { errorMessage } from '@/lib/errors'
import { invalidatePostQueries } from '@/lib/postCache'
import { queryKeys } from '@/lib/queryKeys'

export const profileSchema = z.object({
  displayName: z.string().trim().min(1, 'Nhập tên hiển thị').max(50, 'Tên tối đa 50 ký tự'),
  bio: z.string().max(300, 'Giới thiệu tối đa 300 ký tự'),
})
type FormValues = z.infer<typeof profileSchema>

type Props = { me: Me; open: boolean; onOpenChange: (open: boolean) => void }

export function EditProfileDialog({ me, open, onOpenChange }: Props) {
  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent>
        <DialogTitle>Chỉnh sửa trang cá nhân</DialogTitle>
        <DialogDescription>Tên hiển thị, giới thiệu và ảnh đại diện.</DialogDescription>
        {open && <EditProfileForm me={me} onDone={() => onOpenChange(false)} />}
      </DialogContent>
    </Dialog>
  )
}

function EditProfileForm({ me, onDone }: { me: Me; onDone: () => void }) {
  const queryClient = useQueryClient()
  const fileInput = useRef<HTMLInputElement>(null)
  const [avatarError, setAvatarError] = useState<string | null>(null)
  const form = useForm<FormValues>({
    resolver: zodResolver(profileSchema),
    defaultValues: { displayName: me.displayName, bio: me.bio ?? '' },
  })
  const { errors } = form.formState

  const refresh = (updated: Me) => {
    setSessionUser(queryClient, updated)
    queryClient.invalidateQueries({ queryKey: queryKeys.profile(me.id) })
    invalidatePostQueries(queryClient)
  }

  const profile = useMutation({
    mutationFn: updateMe,
    onSuccess: (updated) => {
      refresh(updated)
      onDone()
    },
  })
  const avatar = useMutation({ mutationFn: uploadAvatar, onSuccess: refresh })

  const onFile = (files: FileList | null) => {
    const file = files?.[0]
    if (fileInput.current) fileInput.current.value = ''
    if (!file) return
    const error = validateImages([], [file])
    setAvatarError(error)
    if (!error) avatar.mutate(file)
  }

  return (
    <div className="grid gap-4">
      <div className="flex items-center gap-4">
        <Avatar name={me.displayName} src={me.avatarUrl} className="size-20 text-2xl" />
        <input
          ref={fileInput}
          type="file"
          accept="image/jpeg,image/png,image/webp"
          hidden
          onChange={(e) => onFile(e.target.files)}
        />
        <Button
          type="button"
          variant="outline"
          disabled={avatar.isPending}
          onClick={() => fileInput.current?.click()}
        >
          <Camera aria-hidden="true" /> {avatar.isPending ? 'Đang tải lên…' : 'Đổi ảnh đại diện'}
        </Button>
      </div>
      {(avatarError || avatar.isError) && (
        <p role="alert" className="text-sm text-destructive">
          {avatarError ?? errorMessage(avatar.error)}
        </p>
      )}

      <form
        noValidate
        className="grid gap-4"
        onSubmit={form.handleSubmit((values) => profile.mutate(values))}
      >
        <FormField id="displayName" label="Tên hiển thị" error={errors.displayName?.message}>
          <Input
            id="displayName"
            aria-invalid={!!errors.displayName}
            {...form.register('displayName')}
          />
        </FormField>
        <FormField id="bio" label="Giới thiệu" error={errors.bio?.message}>
          <Textarea id="bio" aria-invalid={!!errors.bio} {...form.register('bio')} />
        </FormField>
        {profile.isError && (
          <p role="alert" className="text-sm text-destructive">
            {errorMessage(profile.error)}
          </p>
        )}
        <div className="flex justify-end gap-2">
          <Button type="button" variant="outline" onClick={onDone}>
            Đóng
          </Button>
          <Button type="submit" disabled={profile.isPending}>
            Lưu
          </Button>
        </div>
      </form>
    </div>
  )
}
