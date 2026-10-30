<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import SiteHeader from './components/SiteHeader.vue'
import AdminNavigation from './components/AdminNavigation.vue'

const route = useRoute()
const authPage = computed(() => ['/login', '/register', '/forgot'].includes(route.path))
const viewportWidth = ref(window.innerWidth)
const desktopCollapsed = ref(false)
const menuOpen = ref(false)
const navigation = ref(null)
const mobile = computed(() => viewportWidth.value < 600)
const desktop = computed(() => viewportWidth.value >= 1200)
const expanded = computed(() => desktop.value ? !desktopCollapsed.value : menuOpen.value)
const overlay = computed(() => !authPage.value && !desktop.value && menuOpen.value)
const sidebarWidth = computed(() => authPage.value || mobile.value ? '0px' : expanded.value && desktop.value ? '224px' : '64px')
let previousOverflow = ''

function toggleNavigation() {
  if (desktop.value) desktopCollapsed.value = !desktopCollapsed.value
  else menuOpen.value = !menuOpen.value
}
async function closeNavigation(restoreFocus = true) {
  if (!menuOpen.value) return
  menuOpen.value = false
  if (restoreFocus) {
    await nextTick()
    navigation.value?.focusToggle()
  }
}
function resize() {
  const oldMode = mobile.value ? 'mobile' : desktop.value ? 'desktop' : 'rail'
  viewportWidth.value = window.innerWidth
  const newMode = mobile.value ? 'mobile' : desktop.value ? 'desktop' : 'rail'
  if (oldMode !== newMode) closeNavigation()
}
function onEscape(event) {
  if (event.key === 'Escape' && overlay.value) {
    event.preventDefault()
    closeNavigation()
  }
}
watch(() => route.fullPath, async () => {
  const wasOpen = menuOpen.value
  closeNavigation(false)
  if (wasOpen) {
    await nextTick()
    document.getElementById('page-content')?.focus({ preventScroll: true })
  }
})
watch(overlay, value => {
  if (value) {
    previousOverflow = document.body.style.overflow
    document.body.style.overflow = 'hidden'
  } else document.body.style.overflow = previousOverflow
})
onMounted(() => {
  window.addEventListener('resize', resize)
  window.addEventListener('keydown', onEscape)
})
onUnmounted(() => {
  window.removeEventListener('resize', resize)
  window.removeEventListener('keydown', onEscape)
  if (overlay.value) document.body.style.overflow = previousOverflow
})
</script>

<template>
  <div class="app-shell" :class="{ 'auth-shell': authPage }" :style="{ '--sidebar-width': sidebarWidth }">
    <a class="skip-link" href="#page-content">跳到正文</a>
    <SiteHeader v-if="!authPage" ref="navigation" :expanded="expanded" :mobile="mobile" :overlay="overlay"
      @toggle="toggleNavigation" @close="closeNavigation" />
    <div v-if="overlay" class="navigation-scrim" aria-hidden="true" @click="closeNavigation()" />
    <div class="app-body" :inert="overlay ? true : undefined">
      <AdminNavigation v-if="route.meta.requiresAdmin" />
      <div id="page-content" tabindex="-1"><router-view /></div>
    </div>
  </div>
</template>

<style>
html, body, #app { min-height: 100%; margin: 0; }
.app-shell { --shell-top: 0px; min-height: 100dvh; }
.app-body { margin-left: var(--sidebar-width); padding-top: var(--shell-top); min-width: 0; }
#page-content { min-width: 0; }
#page-content:focus { outline: none; }
.navigation-scrim { position: fixed; inset: 0; z-index: 490; background: #1a1a1a45; }
.skip-link { position: fixed; top: -80px; left: 16px; z-index: 3000; background: white; color: #1a1a1a; padding: 12px; border-radius: 8px; }
.skip-link:focus { top: 12px; }
@media (max-width: 599px) {
  .app-shell:not(.auth-shell) { --shell-top: 56px; }
}
</style>
