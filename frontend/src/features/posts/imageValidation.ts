export const MAX_IMAGES = 4
export const MAX_IMAGE_BYTES = 5 * 1024 * 1024
export const ACCEPTED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/webp']

/** Kiểm tra sơ bộ phía client (server vẫn kiểm tra magic bytes). Trả về thông báo lỗi hoặc null. */
export function validateImages(current: File[], added: File[]): string | null {
  if (current.length + added.length > MAX_IMAGES) return `Tối đa ${MAX_IMAGES} ảnh mỗi bài`
  for (const file of added) {
    if (!ACCEPTED_IMAGE_TYPES.includes(file.type))
      return `"${file.name}" không phải ảnh JPEG/PNG/WebP`
    if (file.size > MAX_IMAGE_BYTES) return `"${file.name}" vượt quá 5 MB`
  }
  return null
}
