<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { House, Goods, Plus, ChatDotRound, User, Odometer, QuestionFilled, SwitchButton, Expand, Fold, Close, Menu } from '@element-plus/icons-vue'
import { useNotificationStore } from '../stores/notification'
import { clearAuth, getToken, readUser } from '../utils/session'
import { logout as revokeSession } from '../api/auth'
import { notify } from '../utils/feedback'

const props = defineProps({
  expanded: Boolean,
  mobile: Boolean,
  overlay: Boolean,
})
const emit = defineEmits(['toggle', 'close'])
const route = useRoute()
const router = useRouter()
const user = ref(readUser())
const loggedIn = ref(!!getToken())
const notifications = useNotificationStore()
const panel = ref(null)
const toggleButton = ref(null)
const mobileToggle = ref(null)
const links = [
  { path: '/', label: '首页', icon: House },
  { path: '/products', label: '商品', icon: Goods },
  { path: '/post', label: '发布', icon: Plus },
  { path: '/messages', label: '消息', icon: ChatDotRound },
  { path: '/profile', label: '我的', icon: User },
]
const isAdmin = computed(() => user.value.role === 1 && loggedIn.value)
const unreadLabel = computed(() => notifications.unreadCount > 99 ? '99+' : notifications.unreadCount)
function syncSession() {
  user.value = readUser()
  loggedIn.value = !!getToken()
  if (loggedIn.value) notifications.startPolling(15000)
  else { notifications.stopPolling(); notifications.resetUnreadCount() }
}
function active(path) {
  if (path === '/') return route.path === '/'
  if (path === '/products') return route.path.startsWith('/products') || route.path === '/browse' || route.path.startsWith('/checkout')
  if (path === '/post') return route.path === '/post' || route.path === '/drafts'
  if (path === '/profile') return route.path === '/profile' || route.path.startsWith('/orders')
  return route.path.startsWith(path)
}
async function logout() {
  try { await revokeSession(); clearAuth(); router.push('/login') }
  catch (error) { notify(error.message || '退出失败，请重试') }
}
function focusToggle() { (props.mobile ? mobileToggle.value : toggleButton.value)?.focus() }
function trapFocus(event) {
  if (!props.overlay || event.key !== 'Tab') return
  const targets = [...panel.value.querySelectorAll('a[href], button:not([disabled])')]
  const first = targets[0]
  const last = targets[targets.length - 1]
  if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus() }
  else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus() }
}
watch(() => props.overlay, async open => {
  if (open) { await nextTick(); toggleButton.value?.focus() }
})
watch(() => route.fullPath, syncSession)
onMounted(() => { syncSession(); window.addEventListener('session-changed', syncSession) })
onUnmounted(() => { notifications.stopPolling(); window.removeEventListener('session-changed', syncSession) })
defineExpose({ focusToggle })
</script>

<template>
  <header v-if="mobile" class="mobile-brandbar" :inert="overlay ? true : undefined">
    <RouterLink to="/" class="mobile-brand"><span class="brand-mark" aria-hidden="true">✓</span>校园二手</RouterLink>
    <button ref="mobileToggle" class="nav-toggle" aria-label="打开导航" :aria-expanded="expanded" aria-controls="site-navigation" @click="emit('toggle')"><Menu aria-hidden="true" /></button>
  </header>
  <aside v-show="!mobile || expanded" id="site-navigation" ref="panel" class="site-sidebar"
    :class="{ expanded, 'is-overlay': overlay }" :role="overlay ? 'dialog' : undefined"
    :aria-modal="overlay ? true : undefined" aria-label="校园二手导航" @keydown="trapFocus">
    <div class="sidebar-brand">
      <RouterLink to="/" class="brand-link" aria-label="校园二手首页">
        <span class="brand-mark" aria-hidden="true">✓</span>
        <span class="brand-copy" v-if="expanded"><strong>校园二手</strong><small>让闲置，遇见需要</small></span>
      </RouterLink>
    </div>
    <button ref="toggleButton" class="nav-toggle sidebar-toggle"
      :aria-label="expanded ? '收起导航' : '展开导航'" :aria-expanded="expanded" aria-controls="site-navigation" @click="emit('toggle')">
      <Close v-if="overlay" aria-hidden="true" /><Fold v-else-if="expanded" aria-hidden="true" /><Expand v-else aria-hidden="true" />
      <span v-if="expanded">收起导航</span>
    </button>
    <nav class="sidebar-links" aria-label="主导航">
      <RouterLink v-for="item in links" :key="item.path" :to="item.path" class="sidebar-link"
        :class="{ selected: active(item.path) }" :aria-label="item.label" :aria-current="active(item.path) ? 'page' : undefined">
        <component :is="item.icon" aria-hidden="true" />
        <span class="nav-label">{{ item.label }}</span>
        <span v-if="item.path === '/messages' && notifications.unreadCount" class="nav-badge" :aria-label="notifications.unreadCount + ' 条未读消息'">{{ unreadLabel }}</span>
      </RouterLink>
      <RouterLink v-if="isAdmin" to="/admin" class="sidebar-link admin-entry" :class="{ selected: active('/admin') }"
        aria-label="管理" :aria-current="active('/admin') ? 'page' : undefined">
        <Odometer aria-hidden="true" /><span class="nav-label">管理</span>
      </RouterLink>
    </nav>
    <div class="sidebar-bottom">
      <RouterLink to="/help" class="sidebar-link" aria-label="帮助" :class="{ selected: active('/help') }" :aria-current="active('/help') ? 'page' : undefined">
        <QuestionFilled aria-hidden="true" /><span class="nav-label">使用帮助</span>
      </RouterLink>
      <div class="sidebar-account">
        <template v-if="loggedIn">
          <RouterLink to="/profile" class="sidebar-link account-link" :aria-label="'个人中心：' + user.username">
            <span class="account-avatar" aria-hidden="true">{{ (user.username || '我').slice(0, 1).toUpperCase() }}</span>
            <span class="nav-label account-name">{{ user.username }}</span>
          </RouterLink>
          <button class="sidebar-link logout-link" type="button" aria-label="退出登录" @click="logout">
            <SwitchButton aria-hidden="true" /><span class="nav-label">退出登录</span>
          </button>
        </template>
        <RouterLink v-else to="/login" class="sidebar-link login-link" aria-label="登录">
          <User aria-hidden="true" /><span class="nav-label">登录 / 注册</span>
        </RouterLink>
      </div>
    </div>
  </aside>
</template>

<style scoped>
.site-sidebar { position: fixed; inset: 0 auto 0 0; z-index: 500; width: 64px; box-sizing: border-box; display: flex; flex-direction: column; padding: 24px 10px 16px; background: white; border-right: 1px solid #e8e8e8; color: var(--color-foreground); }
.site-sidebar.expanded { width: 224px; padding-left: 16px; padding-right: 16px; }
.sidebar-brand { margin-bottom: 28px; min-height: 42px; display: flex; align-items: center; }
.brand-link, .mobile-brand { display: flex; align-items: center; gap: 10px; color: inherit; text-decoration: none; }
.brand-link { padding-left: 4px; }
.brand-mark { flex: none; width: 36px; height: 36px; display: grid; place-items: center; background: var(--color-brand); color: #1a1a1a; border-radius: 12px; font-size: 28px; font-weight: 800; }
.brand-copy { display: flex; flex-direction: column; gap: 4px; white-space: nowrap; }
.brand-copy strong { font-size: 19px; letter-spacing: -.5px; }
.brand-copy small { font-size: 11px; color: #777; }
.nav-toggle { border: 0; background: transparent; color: #777; height: 36px; display: flex; align-items: center; justify-content: center; gap: 12px; cursor: pointer; border-radius: 8px; font: inherit; }
.nav-toggle:hover { background: #f5f5f5; color: #1a1a1a; }
.nav-toggle svg { width: 18px; height: 18px; flex: none; }
.sidebar-toggle { width: 100%; margin-bottom: 18px; font-size: 12px; }
.expanded .sidebar-toggle { justify-content: flex-start; padding-left: 13px; }
.sidebar-links { display: flex; flex-direction: column; gap: 7px; }
.sidebar-link { position: relative; display: flex; align-items: center; gap: 14px; min-height: 44px; box-sizing: border-box; padding: 10px 12px; border: 0; border-radius: 9px; background: transparent; color: #686868; text-decoration: none; font: inherit; font-size: 14px; width: 100%; text-align: left; }
.sidebar-link > svg { width: 20px; height: 20px; flex: none; }
.sidebar-link:hover { background: #f5f5f5; color: #1a1a1a; }
.sidebar-link.selected { background: var(--color-brand); color: #1a1a1a; font-weight: 700; }
.nav-label { white-space: nowrap; }
.site-sidebar:not(.expanded) .nav-label { position: absolute; left: 52px; top: 50%; transform: translateY(-50%); background: #1a1a1a; color: white; padding: 7px 10px; border-radius: 6px; font-size: 12px; visibility: hidden; opacity: 0; pointer-events: none; z-index: 2; }
.site-sidebar:not(.expanded) .sidebar-link:hover .nav-label,
.site-sidebar:not(.expanded) .sidebar-link:focus-visible .nav-label { visibility: visible; opacity: 1; }
.nav-badge { margin-left: auto; min-width: 18px; padding: 2px 4px; text-align: center; border-radius: 12px; color: white; background: #1a1a1a; font-size: 10px; line-height: 14px; }
.site-sidebar:not(.expanded) .nav-badge { position: absolute; top: 1px; right: 0; min-width: 13px; padding: 0 3px; }
.admin-entry { margin-top: 14px; }
.sidebar-bottom { margin-top: auto; padding-top: 32px; }
.sidebar-account { border-top: 1px solid #ededed; margin-top: 12px; padding-top: 12px; }
.account-avatar { display: grid; place-items: center; flex: none; width: 24px; height: 24px; margin-left: -2px; border-radius: 50%; background: #f0f0ed; color: #555; font-size: 12px; font-weight: 700; }
.expanded .account-name { overflow: hidden; text-overflow: ellipsis; }
.logout-link { font-size: 12px; }
.logout-link > svg { width: 18px; height: 18px; margin: 0 1px; }
.login-link { color: #1a1a1a; }
.mobile-brandbar { position: fixed; inset: 0 0 auto; height: 56px; z-index: 480; display: flex; align-items: center; justify-content: space-between; padding: 0 16px; background: white; border-bottom: 1px solid #e8e8e8; }
.mobile-brand { font-weight: 700; font-size: 16px; }
.mobile-brand .brand-mark { height: 30px; width: 30px; font-size: 23px; border-radius: 9px; }
.mobile-brandbar .nav-toggle { width: 36px; }
@media (max-height: 640px) { .site-sidebar { padding-top: 12px; overflow-y: auto; } .sidebar-brand { margin-bottom: 12px; } .sidebar-toggle { margin-bottom: 8px; } .sidebar-bottom { padding-top: 12px; } .sidebar-links { gap: 3px; } }
</style>
