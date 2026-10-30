import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getConversations, getConversation, sendMessage as sendApi,
  markAsRead, markAllAsRead } from '../api/message'
import { getUserById } from '../api/user'
import { getProductDetail, splitImages } from '../api/product'
import { useNotificationStore } from '../stores/notification'
import { readUser } from '../utils/session'
import { notify } from '../utils/feedback'
import { buildConversations, readChatDraft, saveChatDraft, sortMessages } from '../utils/chat'

export function useMessages() {
  const route = useRoute(), router = useRouter(), notifications = useNotificationStore()
  const currentUserId = Number(readUser().id) || 0
  const inboxPage = ref(1), inboxTotal = ref(0), inboxPaged = ref(false), unreadTotal = ref(0)
  const hasOlder = ref(false), olderLoading = ref(false)
  let searchTimer
  const conversations = ref([]), selectedUserId = ref(null), currentMessages = ref([]), selectedPeer = ref(null)
  const search = ref(''), activeTab = ref('all'), newMessage = ref('')
  const inboxLoading = ref(true), chatLoading = ref(false), refreshing = ref(false), markingAll = ref(false)
  const inboxError = ref(''), chatError = ref(''), newMessageCount = ref(0), awayFromBottom = ref(false)
  const messagesContainer = ref(null), composer = ref(null)
  const sendingPeers = ref(new Set()), failedSends = ref({})
  const productDetails = ref(null), ignoreProduct = ref(false)
  const localReadIds = new Set(), readJobs = new Set()
  let selectionVersion = 0, inboxVersion = 0, productVersion = 0, timer, stopped = false

  const unreadCount = computed(() => inboxPaged.value ? unreadTotal.value : conversations.value.reduce((sum, peer) => sum + peer.unreadCount, 0))
  const currentConversation = computed(() => conversations.value.find(peer => peer.userId === selectedUserId.value) || (selectedPeer.value?.userId === selectedUserId.value ? selectedPeer.value : null))
  const sending = computed(() => sendingPeers.value.has(selectedUserId.value))
  const failedSend = computed(() => failedSends.value[selectedUserId.value]?.[0])
  const visibleConversations = computed(() => {
    if (inboxPaged.value) return conversations.value
    const term = search.value.trim().toLocaleLowerCase()
    return conversations.value.filter(peer =>
      (activeTab.value !== 'unread' || peer.unreadCount > 0) &&
      (!term || `${peer.username} ${peer.searchText || ''}`.toLocaleLowerCase().includes(term)))
  })
  const contextProduct = computed(() => {
    if (ignoreProduct.value || !selectedUserId.value) return null
    const productId = Number(route.query.productId)
    if (Number(route.query.sellerId) === selectedUserId.value && Number.isSafeInteger(productId) && productId > 0) {
      return { id: productId, title: typeof route.query.productTitle === 'string' ? route.query.productTitle : '咨询的商品' }
    }
    const last = [...currentMessages.value].reverse().find(message => message.productId || message.product?.id)
    return last ? { id: Number(last.product?.id || last.productId), title: last.product?.title || '咨询的商品', price: last.product?.price } : null
  })
  const productCard = computed(() => contextProduct.value ? { ...contextProduct.value,
    ...(productDetails.value?.id === contextProduct.value.id ? productDetails.value : {}) } : null)
  const productImage = computed(() => splitImages(productCard.value?.images)[0] || '')

  watch(unreadCount, value => notifications.setUnreadCount(value), { flush: 'sync' })
  watch(newMessage, value => saveChatDraft(currentUserId, selectedUserId.value, value), { flush: 'sync' })
  watch(() => contextProduct.value?.id, async id => {
    const version = ++productVersion
    productDetails.value = null
    if (!id) return
    try {
      const response = await getProductDetail(id)
      if (!stopped && version === productVersion) productDetails.value = response.data
    } catch { /* Historical messages remain readable when the item is no longer public. */ }
  })

  function nearBottom() {
    const panel = messagesContainer.value
    return !panel || panel.scrollHeight - panel.scrollTop - panel.clientHeight < 64
  }

  async function markVisibleRead() {
    const peerId = selectedUserId.value
    if (stopped || !peerId || chatLoading.value || !nearBottom() || document.visibilityState === 'hidden' || readJobs.has(peerId)) return
    const unread = currentMessages.value.filter(message => Number(message.receiverId) === currentUserId &&
      Number(message.isRead) === 0 && !localReadIds.has(Number(message.id)))
    if (!unread.length) return
    readJobs.add(peerId)
    try {
      const results = await Promise.allSettled(unread.map(message => markAsRead(message.id)))
      let marked = 0
      results.forEach((result, index) => {
        if (result.status === 'fulfilled') {
          const message = unread[index]
          localReadIds.add(Number(message.id))
          message.isRead = 1
          marked++
        }
      })
      if (inboxPaged.value) unreadTotal.value = Math.max(0, unreadTotal.value - marked)
      const peer = conversations.value.find(item => item.userId === peerId)
      if (peer) peer.unreadCount = Math.max(0, peer.unreadCount - marked)
      if (!stopped && peerId === selectedUserId.value && marked < unread.length) chatError.value = '已读状态同步失败，请刷新重试'
    } finally { readJobs.delete(peerId) }
  }

  async function jumpToLatest() {
    const peerId = selectedUserId.value
    await nextTick()
    if (stopped || peerId !== selectedUserId.value) return
    if (messagesContainer.value) messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    newMessageCount.value = 0
    awayFromBottom.value = false
    await markVisibleRead()
  }

  function onMessagesScroll() {
    awayFromBottom.value = !nearBottom()
    if (nearBottom()) {
      newMessageCount.value = 0
      void markVisibleRead()
    }
  }

  async function loadInbox(page = inboxPage.value) {
    inboxPage.value = page
    const version = ++inboxVersion
    try {
      const response = await getConversations({ page, pageSize: 20, search: search.value.trim(), unread: activeTab.value === 'unread' })
      if (stopped || version !== inboxVersion) return
      inboxPaged.value = !Array.isArray(response.data)
      inboxTotal.value = response.data?.total || 0
      unreadTotal.value = response.data?.unreadTotal || 0
      const loaded = inboxPaged.value ? response.data.items : buildConversations(response.data || [], currentUserId, localReadIds)
      // A new seller conversation can exist before its first message is sent.
      const selected = currentConversation.value
      if (selected && !selected.lastMessage && !search.value && activeTab.value === 'all' && !loaded.some(peer => peer.userId === selected.userId)) loaded.unshift(selected)
      conversations.value = loaded
      notifications.setUnreadCount(unreadCount.value)
      inboxError.value = ''
    } catch (error) {
      if (!stopped && version === inboxVersion) inboxError.value = error.message || '会话加载失败，请重试'
    } finally { if (!stopped && version === inboxVersion) inboxLoading.value = false }
  }

  async function loadCurrent(peerId, version, initial = false) {
    const atBottom = initial || nearBottom()
    const previousIds = new Set(currentMessages.value.map(message => Number(message.id)))
    try {
      const after = initial ? null : Math.max(0, ...currentMessages.value.map(message => Number(message.id)))
      const response = await getConversation(peerId, after ? { after } : {})
      if (stopped || version !== selectionVersion || peerId !== selectedUserId.value) return
      if (initial) hasOlder.value = !!response.data?.hasMore
      const receiptIds = new Set(response.data?.readIds || [])
      const loaded = sortMessages(response.data?.items || response.data || []).map(message => localReadIds.has(Number(message.id)) ? { ...message, isRead: 1 } : message)
      // A refresh that started before a send must not remove the just-sent message.
      const messages = sortMessages([...loaded, ...currentMessages.value.filter(message =>
        !loaded.some(row => Number(row.id) === Number(message.id)))])
      const arriving = messages.filter(message => !previousIds.has(Number(message.id)) && Number(message.senderId) !== currentUserId).length
      currentMessages.value = messages.map(message => receiptIds.has(Number(message.id)) ? { ...message, isRead: 1 } : message)
      chatError.value = ''
      chatLoading.value = false
      if (atBottom) await jumpToLatest()
      else newMessageCount.value += arriving
    } catch (error) {
      if (!stopped && version === selectionVersion) chatError.value = error.message || '对话加载失败，请重试'
    } finally { if (!stopped && version === selectionVersion) chatLoading.value = false }
  }

  async function loadOlder() {
    if (olderLoading.value || !hasOlder.value || !currentMessages.value.length) return
    const peer = selectedUserId.value, version = selectionVersion
    const panel = messagesContainer.value, height = panel?.scrollHeight || 0, top = panel?.scrollTop || 0
    olderLoading.value = true
    try {
      const response = await getConversation(peer, { before: Math.min(...currentMessages.value.map(m => Number(m.id))) })
      if (stopped || version !== selectionVersion) return
      currentMessages.value = sortMessages([...(response.data.items || []), ...currentMessages.value])
      hasOlder.value = !!response.data.hasMore
      await nextTick()
      if (panel) panel.scrollTop = top + panel.scrollHeight - height
    } catch (error) { if (version === selectionVersion) chatError.value = error.message }
    finally { olderLoading.value = false }
  }

  watch([search, activeTab], () => {
    clearTimeout(searchTimer)
    // Discard responses from the previous filter immediately.
    inboxVersion++
    searchTimer = setTimeout(() => { void loadInbox(1) }, 250)
  })

  async function selectConversation(peer, keepQuery = false) {
    if (peer.userId === selectedUserId.value && !chatError.value) return
    saveChatDraft(currentUserId, selectedUserId.value, newMessage.value)
    const version = ++selectionVersion
    selectedUserId.value = Number(peer.userId)
    selectedPeer.value = peer
    newMessage.value = readChatDraft(currentUserId, selectedUserId.value)
    currentMessages.value = []
    chatError.value = ''
    newMessageCount.value = 0
    awayFromBottom.value = false
    ignoreProduct.value = false
    chatLoading.value = true
    hasOlder.value = false
    if (!keepQuery && Number(route.query.sellerId) !== selectedUserId.value) {
      void router.replace({ path: '/messages', query: { sellerId: selectedUserId.value } })
    }
    await loadCurrent(selectedUserId.value, version, true)
    if (!stopped && version === selectionVersion) await nextTick(() => composer.value?.focus({ preventScroll: true }))
  }

  async function openFromQuery() {
    const peerId = Number(route.query.sellerId)
    if (!Number.isSafeInteger(peerId) || peerId <= 0) return
    if (peerId === currentUserId) { notify('不能给自己发送消息'); return }
    const version = selectionVersion
    let peer = conversations.value.find(item => item.userId === peerId)
    if (!peer) {
      try {
        const response = await getUserById(peerId)
        if (stopped || version !== selectionVersion || Number(route.query.sellerId) !== peerId) return
        peer = { userId: peerId, username: response.data?.username || `用户${peerId}`,
          avatar: response.data?.avatar || '', unreadCount: 0, lastMessage: '', lastMessageTime: null }
        conversations.value.unshift(peer)
      } catch (error) { inboxError.value = error.message || '暂时无法打开对话'; return }
    }
    await selectConversation(peer, true)
  }

  async function refreshAll(manual = false) {
    if (refreshing.value || stopped || (!manual && document.visibilityState === 'hidden')) return
    refreshing.value = true
    try {
      const peerId = selectedUserId.value, version = selectionVersion
      if (peerId && !chatLoading.value) await loadCurrent(peerId, version)
      await loadInbox()
    } finally { refreshing.value = false }
  }

  function updatePeer(message) {
    const peer = conversations.value.find(item => item.userId === Number(message.receiverId))
    if (!peer) return
    peer.lastMessage = message.content
    peer.lastMessageTime = message.createdTime
    peer.lastSent = true
    peer.searchText = `${peer.searchText || ''} ${message.content}`
    conversations.value = [...conversations.value].sort((a, b) => new Date(b.lastMessageTime) - new Date(a.lastMessageTime))
  }

  async function deliver(payload) {
    const { peerId, content, draft, product } = payload
    if (sendingPeers.value.has(peerId)) return
    sendingPeers.value = new Set([...sendingPeers.value, peerId])
    try {
      const response = await sendApi({ receiverId: peerId, content, productId: product?.id || null, requestKey: payload.requestKey })
      const message = response.data
      if (!message?.id) throw new Error('发送结果不完整，请刷新核对')
      if (stopped) {
        if (readChatDraft(currentUserId, peerId) === draft) saveChatDraft(currentUserId, peerId, '')
        return
      }
      inboxVersion++
      if (product && !message.product) message.product = product
      const saved = peerId === selectedUserId.value ? newMessage.value : readChatDraft(currentUserId, peerId)
      if (saved === draft) {
        saveChatDraft(currentUserId, peerId, '')
        if (peerId === selectedUserId.value) newMessage.value = ''
      }
      failedSends.value[peerId] = (failedSends.value[peerId] || []).filter(item =>
        item.content !== content || item.product?.id !== product?.id)
      updatePeer(message)
      if (peerId === selectedUserId.value) {
        currentMessages.value = sortMessages([...currentMessages.value, message])
        await jumpToLatest()
        composer.value?.focus({ preventScroll: true })
      }
    } catch (error) {
      if (!stopped) {
        const queue = failedSends.value[peerId] || []
        const index = queue.findIndex(item => item.content === content && item.product?.id === product?.id)
        const failed = { ...payload, error: error.message || '发送失败，请重试' }
        if (index < 0) queue.push(failed)
        else queue[index] = failed
        failedSends.value[peerId] = queue
      }
    } finally {
      const peers = new Set(sendingPeers.value)
      peers.delete(peerId)
      sendingPeers.value = peers
    }
  }

  function sendMessage() {
    if (!selectedUserId.value || !newMessage.value.trim() || sending.value) return
    if (newMessage.value.length > 500) { notify('消息不能超过 500 字'); return }
    return deliver({ requestKey: crypto.randomUUID(), peerId: selectedUserId.value, content: newMessage.value.trim(), draft: newMessage.value,
      product: contextProduct.value ? { ...contextProduct.value } : null })
  }

  function onComposerKeydown(event) {
    if (event.key === 'Enter' && !event.shiftKey && !event.isComposing && event.keyCode !== 229) {
      event.preventDefault()
      void sendMessage()
    }
  }

  function addQuickReply(text) {
    const value = newMessage.value ? `${newMessage.value}\n${text}` : text
    if (value.length > 500) { notify('消息不能超过 500 字'); return }
    newMessage.value = value
    composer.value?.focus({ preventScroll: true })
  }

  async function markAllRead() {
    if (markingAll.value || !unreadCount.value) return
    markingAll.value = true
    try {
      await markAllAsRead()
      // Re-read the inbox: a message arriving after the request must stay unread.
      await loadInbox()
      if (selectedUserId.value) await loadCurrent(selectedUserId.value, selectionVersion)
    } catch (error) { notify(error.message || '标记已读失败，请重试') }
    finally { markingAll.value = false }
  }

  function backToConversations() {
    saveChatDraft(currentUserId, selectedUserId.value, newMessage.value)
    selectionVersion++
    selectedUserId.value = null
    currentMessages.value = []
    void router.replace('/messages')
  }

  function onVisibilityChange() { if (document.visibilityState !== 'hidden') void refreshAll() }
  watch(() => [route.query.sellerId, route.query.productId], async ([peerId, productId], previous) => {
    if (inboxLoading.value) return
    if (peerId && Number(peerId) !== selectedUserId.value) await openFromQuery()
    else if (productId !== previous?.[1]) ignoreProduct.value = false
  })
  onMounted(async () => {
    await loadInbox()
    if (stopped) return
    await openFromQuery()
    if (stopped) return
    timer = setInterval(() => { void refreshAll() }, 5000)
    document.addEventListener('visibilitychange', onVisibilityChange)
  })
  onUnmounted(() => {
    saveChatDraft(currentUserId, selectedUserId.value, newMessage.value)
    stopped = true
    selectionVersion++; inboxVersion++; productVersion++
    clearInterval(timer)
    clearTimeout(searchTimer)
    document.removeEventListener('visibilitychange', onVisibilityChange)
  })

  return { inboxPage, inboxTotal, hasOlder, olderLoading, loadOlder, loadInbox, currentUserId, conversations, selectedUserId, currentConversation, currentMessages, visibleConversations,
    search, activeTab, newMessage, unreadCount, inboxLoading, chatLoading, refreshing, markingAll, inboxError, chatError,
    newMessageCount, awayFromBottom, messagesContainer, composer, sending, failedSend, productCard, productImage, ignoreProduct,
    selectConversation, sendMessage, onComposerKeydown, addQuickReply, refreshAll, markAllRead, jumpToLatest,
    onMessagesScroll, backToConversations, retrySend: () => failedSend.value && deliver(failedSend.value),
    goProduct: id => router.push(`/products/${id}`),
    hasDraft: peerId => !!readChatDraft(currentUserId, peerId),
  }
}
