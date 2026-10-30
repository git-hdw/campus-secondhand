import { request } from './http'

export async function getProfile() {
  return await request('/user/profile')
}

export async function updateProfile(profileData) {
  const result = await request('/user/profile', {
    method: 'PUT',
    body: JSON.stringify(profileData)
  })
  const box = localStorage.getItem('token') ? localStorage : sessionStorage
  const { token, ...user } = result.data
  if (token) box.setItem('token', token)
  box.setItem('user', JSON.stringify(user))
  window.dispatchEvent(new Event('session-changed'))
  return result
}

export async function getUserById(userId) {
  return await request(`/user/user/${userId}`)
}

export default { getProfile, updateProfile, getUserById }
