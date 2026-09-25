import type { AxiosAdapter, InternalAxiosRequestConfig } from 'axios'
import { AxiosError, AxiosHeaders } from 'axios'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { api, getAccessToken, setAccessToken, setAuthFailureHandler } from './client'

type Handler = (config: InternalAxiosRequestConfig) => { status: number; data?: unknown }

function useAdapter(handler: Handler) {
  const adapter: AxiosAdapter = async (config) => {
    const { status, data } = handler(config)
    const response = {
      status,
      statusText: String(status),
      data,
      headers: {},
      config,
    }
    if (status >= 400) {
      throw new AxiosError('error', String(status), config, null, response)
    }
    return response
  }
  api.defaults.adapter = adapter
}

const user = { id: 1, email: 'a@b.c', displayName: 'A', bio: null, avatarUrl: null }

describe('api client', () => {
  beforeEach(() => setAccessToken('old-token'))
  afterEach(() => {
    setAccessToken(null)
    setAuthFailureHandler(() => {})
  })

  it('sends bearer token except on public auth endpoints', async () => {
    const seen: Record<string, string | undefined> = {}
    useAdapter((config) => {
      seen[config.url ?? ''] = AxiosHeaders.from(config.headers).get('Authorization') as string
      return { status: 200, data: { accessToken: 'x', user } }
    })
    await api.get('/feed')
    await api.post('/auth/login')
    expect(seen['/feed']).toBe('Bearer old-token')
    expect(seen['/auth/login']).toBeUndefined()
  })

  it('concurrent 401s share a single refresh, then retry with new token', async () => {
    let refreshCalls = 0
    useAdapter((config) => {
      if (config.url === '/auth/refresh') {
        refreshCalls++
        return { status: 200, data: { accessToken: 'new-token', user } }
      }
      const auth = AxiosHeaders.from(config.headers).get('Authorization')
      return auth === 'Bearer new-token' ? { status: 200, data: config.url } : { status: 401 }
    })

    const results = await Promise.all([api.get('/a'), api.get('/b'), api.get('/c')])

    expect(refreshCalls).toBe(1)
    expect(results.map((r) => r.data)).toEqual(['/a', '/b', '/c'])
    expect(getAccessToken()).toBe('new-token')
  })

  it('failed refresh clears token and calls auth failure handler', async () => {
    const onFailure = vi.fn()
    setAuthFailureHandler(onFailure)
    useAdapter(() => ({ status: 401 }))

    await expect(api.get('/feed')).rejects.toBeInstanceOf(AxiosError)
    expect(onFailure).toHaveBeenCalledOnce()
    expect(getAccessToken()).toBeNull()
  })

  it('does not retry non-401 errors', async () => {
    let calls = 0
    useAdapter(() => {
      calls++
      return { status: 403 }
    })
    await expect(api.get('/posts/1')).rejects.toBeInstanceOf(AxiosError)
    expect(calls).toBe(1)
  })
})
