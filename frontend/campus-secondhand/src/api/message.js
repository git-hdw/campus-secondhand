import { request } from './http'

export async function getReceivedMessages() {
  return await request('/messages/received')
}

export async function getSentMessages() {
  return await request('/messages/sent')
}

export async function getConversation(otherUserId, params = {}) {
  return await request(`/messages/conversation/${otherUserId}?` + new URLSearchParams(params))
}

export async function getConversations(params = {}) {
  return await request('/messages/conversations?' + new URLSearchParams(params))
}

export async function getUnreadCount() {
  return await request('/messages/unread-count')
}

export async function sendMessage(params) {
  const { requestKey, ...body } = params
  return await request('/messages/send', {
    method: 'POST',
    headers: requestKey ? { 'Idempotency-Key': requestKey } : {},
    body: JSON.stringify(body)
  })
}

export async function markConversationRead(otherUserId) {
  return await request(`/messages/conversation/${otherUserId}/read`, {
    method: 'PUT'
  })
}

export async function markAsRead(messageId) {
  return await request(`/messages/read/${messageId}`, {
    method: 'PUT'
  })
}

export async function markAllAsRead() {
  return await request('/messages/read-all', {
    method: 'PUT'
  })
}

export async function deleteMessage(messageId) {
  return await request(`/messages/delete/${messageId}`, {
    method: 'DELETE'
  })
}

export default { 
  getReceivedMessages, 
  getSentMessages, 
  getConversation, 
  getUnreadCount,
  sendMessage,
  markConversationRead,
  markAsRead,
  markAllAsRead,
  deleteMessage
}
