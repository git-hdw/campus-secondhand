<template>
  <main class="admin-dashboard">
    <div class="admin-header">
      <div>
        <h1>管理概览</h1>
        <p>欢迎回来，{{ user.username || '管理员' }}。通过上方导航管理平台内容。</p>
      </div>
      <button class="refresh-stats" type="button" :disabled="loading" @click="fetchStats">
        {{ loading ? '正在刷新…' : '刷新数据' }}
      </button>
    </div>

    <div v-if="error" class="stats-error" role="alert">
      <span>{{ error }}</span>
      <button type="button" @click="fetchStats">重新加载</button>
    </div>
    <section class="admin-stats" aria-label="平台数据统计" :aria-busy="loading">
      <div v-for="item in statItems" :key="item.key" class="stat-card">
        <h2>{{ item.label }}</h2>
        <p class="stat-number">{{ loading ? '…' : stats ? stats[item.key] ?? 0 : '—' }}</p>
        <span class="stat-unit">{{ item.unit }}</span>
      </div>
    </section>
  </main>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { getToken, readUser } from '../../utils/session'

const user = readUser()
const stats = ref(null)
const loading = ref(false)
const error = ref('')
const statItems = [
  { key: 'userCount', label: '用户总数', unit: '位用户' },
  { key: 'productCount', label: '商品总数', unit: '件商品' },
  { key: 'orderCount', label: '订单总数', unit: '笔订单' },
  { key: 'messageCount', label: '消息总数', unit: '条消息' },
]

async function fetchStats() {
  if (loading.value) return
  loading.value = true
  error.value = ''
  try {
    const response = await axios.get('/api/admin/stats', {
      headers: { Authorization: 'Bearer ' + getToken() },
    })
    if (response.data?.code !== 200 || !response.data.data) {
      throw new Error(response.data?.message || '统计数据加载失败')
    }
    stats.value = response.data.data
  } catch (err) {
    stats.value = null
    error.value = err.response?.data?.message || err.message || '统计数据加载失败，请重试'
  } finally {
    loading.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.admin-dashboard { min-height: calc(100vh - 360px); box-sizing: border-box; }
.admin-header { display: flex; justify-content: space-between; align-items: center; gap: 24px; }
.admin-header h1 { margin: 8px 0 12px; }
.admin-header p { margin: 0; font-size: 14px; line-height: 1.8; color: #77776c !important; }
.refresh-stats { flex-shrink: 0; min-height: 38px; padding: 9px 16px; background: white; border: 1px solid #deded5; border-radius: 8px; cursor: pointer; }
.refresh-stats:hover { background: #fafaf6; border-color: #aaa99c; }
.refresh-stats:disabled { cursor: wait; opacity: .6; }
.admin-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 20px; margin-top: 28px; }
.stat-card { padding: 24px; border-radius: 12px; }
.stat-card h2 { margin: 0; font-size: 14px; font-weight: 500; color: #77776c; }
.stat-number { margin: 22px 0 6px; font-size: 36px; line-height: 1.2; font-weight: 700; font-variant-numeric: tabular-nums; }
.stat-unit { font-size: 12px; color: #89897e; }
.stats-error { display: flex; align-items: center; gap: 16px; padding: 14px 18px; background: #fff4ed; border-radius: 8px; color: #923e26; }
.stats-error button { margin-left: auto; padding: 6px 10px; background: white; border: 1px solid #e3c4b7; border-radius: 6px; cursor: pointer; }
@media (max-width: 760px) {
  .admin-header { align-items: flex-start; }
  .admin-stats { grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 12px; }
}
</style>
