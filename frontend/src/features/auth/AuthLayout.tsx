import type { ReactNode } from 'react'
import { Card } from '@/components/ui/card'

export function AuthLayout({ title, children }: { title: string; children: ReactNode }) {
  return (
    <main className="grid min-h-svh place-items-center p-4">
      <div className="w-full max-w-sm">
        <p className="mb-6 text-center text-3xl font-bold text-primary">mini-facebook</p>
        <Card className="p-6">
          <h1 className="mb-4 text-xl font-semibold">{title}</h1>
          {children}
        </Card>
      </div>
    </main>
  )
}
