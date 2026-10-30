export function getToken() {
  return localStorage.getItem('token') || sessionStorage.getItem('token')
}

export function readUser() {
  try {
    const user = JSON.parse(localStorage.getItem('user') || sessionStorage.getItem('user') || '{}')
    return user && typeof user === 'object' && !Array.isArray(user) ? user : {}
  } catch {
    return {}
  }
}

export function clearAuth() {
  for (const box of [localStorage, sessionStorage]) {
    for (const key of ['token', 'user', 'username', 'justLoggedIn']) box.removeItem(key)
  }
  window.dispatchEvent(new Event('session-changed'))
}

export function dropExpiredSession() {
  if (!getToken()) return
  clearAuth()
  if (!location.pathname.startsWith('/login')) {
    const redirect = encodeURIComponent(location.pathname + location.search)
    location.assign(`/login?redirect=${redirect}`)
  }
}
