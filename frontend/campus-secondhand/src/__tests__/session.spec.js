import { beforeEach, describe, expect, it, vi } from 'vitest'
import { clearAuth, readUser } from '../utils/session'
import { request } from '../api/http'

beforeEach(() => { localStorage.clear(); sessionStorage.clear(); vi.restoreAllMocks() })

describe('session and request handling', () => {
  it('clears both persistence modes so a previous account cannot override the next one', () => {
    for (const box of [localStorage, sessionStorage]) {
      box.setItem('token', 'old-token'); box.setItem('user', '{"role":1}')
    }
    clearAuth()
    expect(localStorage.getItem('token')).toBeNull()
    expect(sessionStorage.getItem('token')).toBeNull()
    expect(readUser()).toEqual({})
  })
  it('survives corrupted saved user data', () => {
    for (const raw of ['broken', 'null', '[]']) {
      localStorage.setItem('user', raw)
      expect(readUser()).toEqual({})
    }
  })
  it('preserves server messages for parameter failures', async () => {
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false, status:400, json:async () => ({ code:400, message:'价格必须大于 0' })}))
    await expect(request('/products/create')).rejects.toThrow('价格必须大于 0')
  })
  it('clears a disabled account instead of leaving stale admin controls', async () => {
    sessionStorage.setItem('token', 'disabled')
    vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ok:false, status:403, json:async () => ({ code:403, message:'账户已被禁用' })}))
    await expect(request('/user/profile')).rejects.toThrow('账户已被禁用')
    expect(sessionStorage.getItem('token')).toBeNull()
  })
  it('gives a retryable network message', async () => {
    vi.stubGlobal('fetch', vi.fn().mockRejectedValue(new TypeError('Failed to fetch')))
    await expect(request('/products/list')).rejects.toThrow('暂时无法连接服务器')
  })
})
