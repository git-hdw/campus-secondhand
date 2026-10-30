<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { request } from '../api/http'
import { clearAuth } from '../utils/session'
const router = useRouter()
const rows = ref([]), error = ref(''), busy = ref(false)
async function load() {
  busy.value = true; error.value = ''
  try { rows.value = (await request('/auth/sessions')).data }
  catch (e) { error.value = e.message }
  finally { busy.value = false }
}
async function revoke(row) {
  busy.value = true; error.value = ''
  try {
    await request(`/auth/sessions/${row.id}`, { method: 'DELETE' })
    if (row.current) { clearAuth(); await router.push('/login') }
    else await load()
  } catch (e) { error.value = e.message }
  finally { busy.value = false }
}
function date(value) { return new Date(value).toLocaleString('zh-CN', { hour12: false }) }
async function revokeAll() {
  busy.value = true; error.value = ''
  try { await request('/auth/logout-all', { method: 'POST' }); clearAuth(); await router.push('/login') }
  catch (e) { error.value = e.message }
  finally { busy.value = false }
}
</script>
<template>
  <details class="session-panel" @toggle="event => event.target.open && load()">
    <summary>登录会话管理</summary>
    <p>可退出其他登录会话。每次登录单独记录；修改密码会使旧会话全部失效。</p>
    <p v-if="error" role="alert">{{ error }} <button type="button" @click="load">重试</button></p>
    <p v-if="busy" role="status">正在处理…</p>
    <button type="button" :disabled="busy" @click="revokeAll">退出所有会话（包括当前）</button>
    <ul><li v-for="row in rows" :key="row.id"><span>{{ row.current ? '当前会话' : '其他会话' }} · {{ date(row.createdAt) }} 登录<br /><small>{{ date(row.expiresAt) }} 到期</small></span><button type="button" :disabled="busy" @click="revoke(row)">退出{{ row.current ? '当前' : '此' }}会话</button></li></ul>
  </details>
</template>
<style scoped>
.session-panel { margin: 16px 0; border: 1px solid var(--color-border, #ddd); border-radius: 12px; padding: 14px 18px; background: white; font-size: 14px; }
summary { cursor: pointer; font-weight: 600; }
p, small { color: var(--color-text-secondary, #666); }
ul { padding: 0; list-style: none; } li { display: flex; justify-content: space-between; align-items: center; gap: 16px; padding: 12px 0; border-top: 1px solid #eee; }
button { padding: 8px 12px; border: 1px solid #ddd; border-radius: 8px; background: white; cursor: pointer; }
</style>
