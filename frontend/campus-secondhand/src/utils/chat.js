export function sortMessages(messages) {
  const unique = new Map(messages.map(message => [Number(message.id), message]))
  return [...unique.values()].sort((a, b) =>
    new Date(a.createdTime) - new Date(b.createdTime) || Number(a.id) - Number(b.id))
}

export function buildConversations(messages, currentUserId, readIds = new Set()) {
  const peers = new Map()
  for (const message of sortMessages(messages)) {
    const sent = Number(message.senderId) === currentUserId
    const userId = Number(sent ? message.receiverId : message.senderId)
    const user = (sent ? message.receiver : message.sender) || {}
    if (!peers.has(userId)) peers.set(userId, { userId, username: user.username || `用户${userId}`,
      avatar: user.avatar || '', unreadCount: 0, searchText: '' })
    const peer = peers.get(userId)
    if (user.username) peer.username = user.username
    if (user.avatar) peer.avatar = user.avatar
    peer.lastMessage = message.content
    peer.lastMessageTime = message.createdTime
    peer.lastProduct = message.product || peer.lastProduct
    peer.lastSent = sent
    peer.searchText += ` ${message.content || ''} ${message.product?.title || ''}`
    if (!sent && Number(message.isRead) === 0 && !readIds.has(Number(message.id))) peer.unreadCount++
  }
  return [...peers.values()].sort((a, b) => new Date(b.lastMessageTime) - new Date(a.lastMessageTime))
}

function draftKey(userId, peerId) { return `chat_draft_${userId}_${peerId}` }

export function readChatDraft(userId, peerId) {
  try { return sessionStorage.getItem(draftKey(userId, peerId)) || '' } catch { return '' }
}

export function saveChatDraft(userId, peerId, text) {
  if (!userId || !peerId) return
  try {
    if (text) sessionStorage.setItem(draftKey(userId, peerId), text)
    else sessionStorage.removeItem(draftKey(userId, peerId))
  } catch { /* Keep the editable text usable when browser storage is unavailable. */ }
}

export function sameDay(left, right) {
  return new Date(left).toDateString() === new Date(right).toDateString()
}

export function dateLabel(value) {
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return ''
  const now = new Date()
  if (sameDay(date, now)) return '今天'
  const yesterday = new Date(now)
  yesterday.setDate(now.getDate() - 1)
  if (sameDay(date, yesterday)) return '昨天'
  return date.toLocaleDateString('zh-CN', { month: 'long', day: 'numeric',
    ...(date.getFullYear() === now.getFullYear() ? {} : { year: 'numeric' }) })
}

export function clockTime(value) {
  const date = new Date(value)
  return Number.isNaN(date.getTime()) ? '' : date.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit', hour12: false })
}

export function previewTime(value) {
  if (!value) return ''
  return sameDay(value, new Date()) ? clockTime(value) : dateLabel(value)
}
