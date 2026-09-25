import { describe, expect, it } from 'vitest'
import { validateImages } from './imageValidation'

function file(name: string, type: string, size = 100) {
  return new File([new Uint8Array(size)], name, { type })
}

describe('validateImages', () => {
  it('accepts up to 4 supported images', () => {
    const images = [file('a.jpg', 'image/jpeg'), file('b.png', 'image/png')]
    expect(validateImages([], images)).toBeNull()
    expect(validateImages(images, [file('c.webp', 'image/webp')])).toBeNull()
  })

  it('rejects a fifth image', () => {
    const four = Array.from({ length: 4 }, (_, i) => file(`${i}.jpg`, 'image/jpeg'))
    expect(validateImages(four, [file('5.jpg', 'image/jpeg')])).toBe('Tối đa 4 ảnh mỗi bài')
  })

  it('rejects unsupported type and files over 5 MB', () => {
    expect(validateImages([], [file('x.gif', 'image/gif')])).toContain('không phải ảnh')
    expect(validateImages([], [file('big.jpg', 'image/jpeg', 5 * 1024 * 1024 + 1)])).toContain(
      'vượt quá 5 MB',
    )
  })
})
