import { describe, expect, it } from 'vitest'
import { formatFull, formatRelative } from './time'

describe('time formatting', () => {
  const now = new Date('2026-09-25T10:00:00Z')

  it('formats recent times relatively in Vietnamese', () => {
    expect(formatRelative('2026-09-25T09:59:30Z', now)).toBe('Vừa xong')
    expect(formatRelative('2026-09-25T09:55:00Z', now)).toBe('5 phút trước')
    expect(formatRelative('2026-09-25T07:00:00Z', now)).toBe('3 giờ trước')
    expect(formatRelative('2026-09-24T10:00:00Z', now)).toBe('Hôm qua')
  })

  it('shows date in Vietnam time zone after a week', () => {
    // 17:30 UTC ngày 1/9 là 00:30 ngày 2/9 giờ Việt Nam
    expect(formatRelative('2026-09-01T17:30:00Z', now)).toContain('2 tháng 9')
  })

  it('full format uses Vietnam time zone', () => {
    expect(formatFull('2026-09-01T17:30:00Z')).toContain('00:30')
  })
})
