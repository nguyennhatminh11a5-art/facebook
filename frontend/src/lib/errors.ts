import { isAxiosError } from 'axios'

type ProblemDetail = { detail?: string; errors?: Record<string, string> }

/** Thông báo lỗi hiển thị cho người dùng từ ProblemDetail của backend. */
export function errorMessage(
  error: unknown,
  fallback = 'Đã có lỗi xảy ra, vui lòng thử lại',
): string {
  if (isAxiosError<ProblemDetail>(error)) {
    if (!error.response) return 'Không kết nối được máy chủ'
    const status = error.response.status
    if (status === 413) return 'Ảnh quá lớn (tối đa 5 MB mỗi ảnh)'
    if (status === 429) return 'Bạn thao tác quá nhanh, vui lòng thử lại sau'
    const detail = error.response.data?.detail
    if (detail) return detail
  }
  return fallback
}

export function httpStatus(error: unknown): number | undefined {
  return isAxiosError(error) ? error.response?.status : undefined
}
