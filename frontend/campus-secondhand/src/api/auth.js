import { clearAuth, dropExpiredSession } from '../utils/session'
import { request as authenticatedRequest } from './http'
const BASE = import.meta.env.VITE_API_BASE ? `${import.meta.env.VITE_API_BASE}/auth` : '/api/auth'

async function request(path, options = {}) {
  try {
    const res = await fetch(`${BASE}${path}`, {
      headers: { 'Content-Type': 'application/json' },
      ...options,
    })
    const data = await res.json().catch(() => ({ message: '服务器返回非JSON' }))
    if (!res.ok) {
      const err = new Error(data?.message || `HTTP ${res.status}`)
      err.response = data
      throw err
    }

    if (data && typeof data.code === 'number') {
      if (data.code !== 200) {
        const err = new Error(data.message || '请求未成功')
        err.response = data
        throw err
      } else {
        // 后端返回的结构是 { code: 200, message: '成功', data: {...} }
        // 所以直接返回data.data部分
        return data.data
      }
    }

    return data
  } catch (e) {
    if (e && e.response) throw e
    const err = new Error('Network request failed')
    err.cause = e
    throw err
  }
}

export { clearAuth, dropExpiredSession }

export async function login({ username, password }) {
  return await request('/login', { method: 'POST', body: JSON.stringify({ username, password }) })
}

export async function register({ username, email, password, confirmPassword, studentNo }) {
  return await request('/register', { method: 'POST', body: JSON.stringify({ username, email, password, confirmPassword, studentNo }) })
}

export async function forgotPassword({ email }) {
  const body = new URLSearchParams()
  body.append('email', email)
  return await request('/forgot-password', { method: 'POST', headers: { 'Content-Type': 'application/x-www-form-urlencoded' }, body })
}

export async function logout() {
  return await authenticatedRequest('/auth/logout', { method: 'POST' })
}

export default { login, register, forgotPassword, logout, clearAuth }
