import { ref } from 'vue'
import { defineStore } from 'pinia'
import { getUnreadCount } from '../api/message'

export const useNotificationStore = defineStore('notification', () => {
  const unreadCount = ref(0)
  const isPolling = ref(false)
  let pollInterval = ref(null)

  async function fetchUnreadCount() {
    try {
      const res = await getUnreadCount()
      if (res.code === 200 && res.data) {
        unreadCount.value = typeof res.data === 'object' ? (res.data.count || 0) : (res.data || 0)
      }
    } catch (e) {
      console.error('获取未读消息数量失败:', e)
    }
  }

  function startPolling(interval = 30000) {
    if (isPolling.value) return
    
    isPolling.value = true
    fetchUnreadCount()
    
    pollInterval.value = setInterval(() => {
      fetchUnreadCount()
    }, interval)
  }

  function stopPolling() {
    isPolling.value = false
    if (pollInterval.value) {
      clearInterval(pollInterval.value)
      pollInterval.value = null
    }
  }

  function setUnreadCount(count) {
    unreadCount.value = count
  }

  function decrementUnreadCount() {
    if (unreadCount.value > 0) {
      unreadCount.value--
    }
  }

  function resetUnreadCount() {
    unreadCount.value = 0
  }

  return {
    unreadCount,
    isPolling,
    fetchUnreadCount,
    startPolling,
    stopPolling,
    setUnreadCount,
    decrementUnreadCount,
    resetUnreadCount
  }
})

