<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElDialog } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import AdminPagination from '../../components/AdminPagination.vue'
import AdminListState from '../../components/AdminListState.vue'
import { useAdminList, adminHeaders, requireAdminData, adminError } from '../../composables/useAdminList'
import { adminDate, adminTime } from '../../utils/adminFormat'
import { notify, confirmAction } from '../../utils/feedback'

const searchQuery = ref(''), busy = ref(null), selectedMessage = ref(null), showMessage = ref(false)
const { items: messages, total, page, pageSize, totalPages, loading, error, load, applyFilters, changePage, resizePage } =
  useAdminList('/api/admin/messages', () => ({ search: searchQuery.value.trim() }))
function resetSearch() { searchQuery.value = ''; applyFilters() }
function viewMessage(message) { selectedMessage.value = message; showMessage.value = true }
async function deleteMessage(message) {
  if (busy.value !== null) return
  busy.value = message.id
  try {
    if (!await confirmAction('删除后无法恢复这条消息，交易双方也将无法再查看。确认删除吗？', '删除消息', '确认删除')) return
    requireAdminData(await axios.delete('/api/admin/messages/' + message.id, { headers: adminHeaders() }))
    notify('消息已删除')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = null }
}
</script>

<template>
  <main class="admin-messages admin-page">
    <div class="admin-page-heading"><div><h1>消息管理</h1><p class="admin-page-description">查看站内沟通记录，处理需要移除的消息。</p></div><span class="admin-total">共 {{ total }} 条消息</span></div>
    <section class="admin-panel">
      <form class="admin-toolbar" role="search" @submit.prevent="applyFilters">
        <div class="admin-search-field"><Search aria-hidden="true" /><input v-model="searchQuery" class="search-input" aria-label="搜索消息" placeholder="搜索消息内容" /></div>
        <button type="submit" class="admin-btn primary">搜索</button><button type="button" class="admin-btn ghost" @click="resetSearch"><RefreshRight aria-hidden="true" />重置</button>
      </form>
      <AdminListState :loading="loading" :error="error" :empty="!messages.length" empty-text="没有找到符合条件的消息" @retry="load">
        <div class="admin-table-scroll"><table class="admin-table">
          <thead><tr><th scope="col">消息内容</th><th scope="col">会话双方</th><th scope="col">发送时间</th><th scope="col">操作</th></tr></thead>
          <tbody><tr v-for="message in messages" :key="message.id">
            <td><span class="admin-message-excerpt">{{ message.content }}</span><span class="admin-cell-sub">消息 #{{ message.id }}</span></td>
            <td><span>{{ message.sender?.username || '未知用户' }}</span><span class="admin-cell-secondary">发给 {{ message.receiver?.username || '未知用户' }}</span></td>
            <td class="admin-date">{{ adminDate(message.createdTime) }}<span class="admin-cell-sub">{{ adminTime(message.createdTime) }}</span></td>
            <td><div class="admin-row-actions"><button class="admin-btn small" @click="viewMessage(message)">查看内容</button><button class="admin-btn danger small" :disabled="busy !== null" @click="deleteMessage(message)">{{ busy === message.id ? '处理中…' : '删除' }}</button></div></td>
          </tr></tbody>
        </table></div>
      </AdminListState>
      <AdminPagination v-if="!error" :page="page" :page-size="pageSize" :total="total" :total-pages="totalPages" :loading="loading" @change="changePage" @resize="resizePage" />
    </section>
    <ElDialog v-model="showMessage" title="消息内容" width="560px" class="admin-detail-dialog" align-center>
      <template v-if="selectedMessage"><div class="admin-cell-sub">{{ selectedMessage.sender?.username || '未知用户' }} 发给 {{ selectedMessage.receiver?.username || '未知用户' }} · {{ adminDate(selectedMessage.createdTime) }} {{ adminTime(selectedMessage.createdTime) }}</div><p class="admin-detail-description">{{ selectedMessage.content }}</p></template>
      <template #footer><button class="admin-btn" @click="showMessage = false">关闭</button></template>
    </ElDialog>
  </main>
</template>
