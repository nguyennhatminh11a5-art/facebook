import { QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { describe, expect, it } from 'vitest'
import type { FriendStatus } from '@/api/types'
import { createQueryClient } from '@/lib/queryClient'
import { FriendActionButton } from './FriendActionButton'

function renderStatus(status: FriendStatus) {
  return render(
    <QueryClientProvider client={createQueryClient()}>
      <FriendActionButton userId={2} status={status} />
    </QueryClientProvider>,
  )
}

describe('FriendActionButton', () => {
  it.each<[FriendStatus, string[]]>([
    ['NONE', ['Kết bạn']],
    ['OUTGOING', ['Huỷ lời mời']],
    ['INCOMING', ['Chấp nhận', 'Từ chối']],
    ['FRIENDS', ['Bạn bè']],
  ])('status %s shows %j', (status, labels) => {
    renderStatus(status)
    for (const label of labels) {
      expect(screen.getByRole('button', { name: label })).toBeInTheDocument()
    }
  })

  it('renders nothing for own profile', () => {
    const { container } = renderStatus('SELF')
    expect(container).toBeEmptyDOMElement()
  })
})
