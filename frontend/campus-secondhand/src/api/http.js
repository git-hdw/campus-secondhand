import { clearAuth, dropExpiredSession, getToken } from '../utils/session'

const BASE = import.meta.env.VITE_API_BASE || '/api'

export async function request(path, options = {}) {
  const controller = new AbortController()
  const timeout = setTimeout(() => controller.abort(), 15000)
  try {
    const token = getToken()
    const res = await fetch(`${BASE}${path}`, {
      ...options,
      signal: options.signal || controller.signal,
      headers: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...options.headers,
      },
    })
    const data = await res.json().catch(() => ({ message: '服务器返回格式不正确，请重试' }))
    if (res.status === 401 || data.code === 401) dropExpiredSession()
    if ((res.status === 403 || data.code === 403) && data.message?.includes('禁用')) clearAuth()
    if (!res.ok || typeof data.code !== 'number' || data.code !== 200) {
      const error = new Error(data.message || `请求失败（${res.status}）`)
      error.response = data
      error.status = res.status
      throw error
    }
    return data
  } catch (error) {
    if (error.response) throw error
    if (error.name === 'AbortError') throw new Error('请求超时，请检查网络后重试')
    throw new Error('暂时无法连接服务器，请稍后重试')
  } finally {
    clearTimeout(timeout)
  }
}
