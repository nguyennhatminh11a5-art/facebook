const relative = new Intl.RelativeTimeFormat('vi', { numeric: 'auto' })
const full = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  dateStyle: 'full',
  timeStyle: 'short',
})
const short = new Intl.DateTimeFormat('vi-VN', {
  timeZone: 'Asia/Ho_Chi_Minh',
  day: 'numeric',
  month: 'long',
  year: 'numeric',
})

const UNITS: Array<[Intl.RelativeTimeFormatUnit, number]> = [
  ['year', 365 * 24 * 3600],
  ['month', 30 * 24 * 3600],
  ['week', 7 * 24 * 3600],
  ['day', 24 * 3600],
  ['hour', 3600],
  ['minute', 60],
]

/** "3 phút trước", "hôm qua"...; quá 1 tuần thì hiện ngày theo giờ Việt Nam. */
export function formatRelative(iso: string, now: Date = new Date()): string {
  const date = new Date(iso)
  const seconds = Math.round((date.getTime() - now.getTime()) / 1000)
  const abs = Math.abs(seconds)
  if (abs < 60) return 'Vừa xong'
  if (abs >= 7 * 24 * 3600) return short.format(date)
  for (const [unit, size] of UNITS) {
    if (abs >= size) return relative.format(Math.round(seconds / size), unit)
  }
  return relative.format(seconds, 'second')
}

/** Thời gian đầy đủ theo giờ Việt Nam (tooltip). */
export function formatFull(iso: string): string {
  return full.format(new Date(iso))
}
