<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import AdminPagination from '../../components/AdminPagination.vue'
import AdminListState from '../../components/AdminListState.vue'
import { useAdminList, adminHeaders, requireAdminData, adminError } from '../../composables/useAdminList'
import { adminDate, adminTime } from '../../utils/adminFormat'
import { readUser } from '../../utils/session'
import { notify, confirmAction } from '../../utils/feedback'

const searchQuery = ref('')
const currentUser = readUser()
const busy = ref(null)
const { items: users, total, page, pageSize, totalPages, loading, error, load, applyFilters, changePage, resizePage } =
  useAdminList('/api/admin/users', () => ({ search: searchQuery.value.trim() }))
function resetSearch() { searchQuery.value = ''; applyFilters() }

async function toggleStatus(user) {
  if (busy.value !== null || user.id === currentUser.id) return
  const status = user.status === 1 ? 0 : 1
  busy.value = user.id
  try {
    if (status === 0 && !await confirmAction('禁用后，该账号将无法登录和继续操作。确认禁用 ' + user.username + ' 吗？', '禁用账号', '确认禁用')) return
    requireAdminData(await axios.put('/api/admin/users/' + user.id + '/status', status, { headers: { ...adminHeaders(), 'Content-Type': 'application/json' } }))
    notify(status === 0 ? '账号已禁用' : '账号已启用')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = null }
}
</script>

<template>
  <main class="admin-users admin-page">
    <div class="admin-page-heading">
      <div><h1>用户管理</h1><p class="admin-page-description">查看校园账号，管理用户的使用状态。</p></div>
      <span class="admin-total">共 {{ total }} 位用户</span>
    </div>
    <section class="admin-panel">
      <form class="admin-toolbar" role="search" @submit.prevent="applyFilters">
        <div class="admin-search-field"><Search aria-hidden="true" /><input v-model="searchQuery" class="search-input" aria-label="搜索用户" placeholder="搜索用户名或邮箱" /></div>
        <button type="submit" class="admin-btn primary search-btn">搜索</button>
        <button type="button" class="admin-btn ghost" @click="resetSearch"><RefreshRight aria-hidden="true" />重置</button>
        <span class="admin-toolbar-hint">账号状态变更后即时生效</span>
      </form>
      <AdminListState :loading="loading" :error="error" :empty="!users.length" empty-text="没有找到符合条件的用户" @retry="load">
        <div class="admin-table-scroll users-table-container">
          <table class="admin-table">
            <thead><tr><th scope="col">用户</th><th scope="col">联系方式</th><th scope="col">身份</th><th scope="col">账号状态</th><th scope="col">注册时间</th><th scope="col">操作</th></tr></thead>
            <tbody><tr v-for="user in users" :key="user.id">
              <td><div class="admin-identity"><span class="admin-avatar" aria-hidden="true"><img v-if="user.avatar" :src="user.avatar" alt="" /><template v-else>{{ (user.username || '?').slice(0, 1).toUpperCase() }}</template></span><div><span class="admin-cell-title">{{ user.username }}</span><span class="admin-cell-sub">ID {{ user.id }}<template v-if="user.studentNo"> · {{ user.studentNo }}</template></span></div></div></td>
              <td><span>{{ user.email || '未填写邮箱' }}</span><span class="admin-cell-secondary">{{ user.phone || '未填写手机号' }}</span></td>
              <td><span class="admin-role" :class="{ manager: user.role === 1 }">{{ user.role === 1 ? '管理员' : '学生用户' }}</span></td>
              <td><span class="admin-status" :class="user.status === 1 ? 'good' : 'neutral'">{{ user.status === 1 ? '正常' : '已禁用' }}</span></td>
              <td class="admin-date">{{ adminDate(user.createdTime) }}<span class="admin-cell-sub">{{ adminTime(user.createdTime) }}</span></td>
              <td><span v-if="user.id === currentUser.id" class="admin-cell-sub">当前账号</span><button v-else class="admin-btn small" :class="user.status === 1 ? 'danger' : 'positive'" :disabled="busy !== null" :aria-label="(user.status === 1 ? '禁用 ' : '启用 ') + user.username" @click="toggleStatus(user)">{{ busy === user.id ? '处理中…' : user.status === 1 ? '禁用账号' : '启用账号' }}</button></td>
            </tr></tbody>
          </table>
        </div>
      </AdminListState>
      <AdminPagination v-if="!error" :page="page" :page-size="pageSize" :total="total" :total-pages="totalPages" :loading="loading" @change="changePage" @resize="resizePage" />
    </section>
  </main>
</template>
