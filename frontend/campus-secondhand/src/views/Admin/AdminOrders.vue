<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import AdminPagination from '../../components/AdminPagination.vue'
import AdminListState from '../../components/AdminListState.vue'
import { useAdminList, adminHeaders, requireAdminData, adminError } from '../../composables/useAdminList'
import { adminDate, adminTime, adminMoney } from '../../utils/adminFormat'
import { notify, confirmAction } from '../../utils/feedback'

const searchQuery = ref(''), statusFilter = ref(''), busy = ref(null)
const statuses = { 0: '待见面', 3: '已完成', 4: '已取消' }
const { items: orders, total, page, pageSize, totalPages, loading, error, load, applyFilters, changePage, resizePage } =
  useAdminList('/api/admin/orders', () => ({ search: searchQuery.value.trim(), ...(statusFilter.value !== '' ? { status: statusFilter.value } : {}) }))
function resetSearch() { searchQuery.value = ''; statusFilter.value = ''; applyFilters() }
async function deleteOrder(order) {
  if (busy.value !== null || order.status !== 4) return
  busy.value = order.id
  try {
    if (!await confirmAction('删除后无法恢复这条已取消订单的记录。确认删除吗？', '删除订单记录', '确认删除')) return
    requireAdminData(await axios.delete('/api/admin/orders/' + order.id, { headers: adminHeaders() }))
    notify('订单记录已删除')
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = null }
}
</script>

<template>
  <main class="admin-orders admin-page">
    <div class="admin-page-heading"><div><h1>订单管理</h1><p class="admin-page-description">跟进校内见面交易，查看订单流转记录。</p></div><span class="admin-total">共 {{ total }} 笔订单</span></div>
    <section class="admin-panel">
      <form class="admin-toolbar" role="search" @submit.prevent="applyFilters">
        <div class="admin-search-field"><Search aria-hidden="true" /><input v-model="searchQuery" class="search-input" aria-label="搜索订单" placeholder="输入订单编号" /></div>
        <select v-model="statusFilter" class="admin-select" aria-label="订单状态" @change="applyFilters"><option value="">全部状态</option><option value="0">待见面</option><option value="3">已完成</option><option value="4">已取消</option></select>
        <button class="admin-btn primary" type="submit">搜索</button><button class="admin-btn ghost" type="button" @click="resetSearch"><RefreshRight aria-hidden="true" />重置</button>
        <span class="admin-toolbar-hint">收货由买家本人确认</span>
      </form>
      <AdminListState :loading="loading" :error="error" :empty="!orders.length" empty-text="没有找到符合条件的订单" @retry="load">
        <div class="admin-table-scroll"><table class="admin-table">
          <thead><tr><th scope="col">商品 / 订单编号</th><th scope="col">交易双方</th><th scope="col">订单金额</th><th scope="col">订单状态</th><th scope="col">下单时间</th><th scope="col">操作</th></tr></thead>
          <tbody><tr v-for="order in orders" :key="order.id">
            <td><span class="admin-cell-title">{{ order.productTitle || '商品已删除' }}</span><span class="admin-cell-sub admin-order-no" :title="order.orderNo">{{ order.orderNo || '#' + order.id }}</span></td>
            <td><span>买家：{{ order.buyerName || '未知买家' }}</span><span class="admin-cell-secondary">卖家：{{ order.sellerName || '未知卖家' }}</span></td>
            <td><span class="admin-price">¥{{ adminMoney(order.price) }}</span></td>
            <td><span class="admin-status" :class="order.status === 0 ? 'waiting' : order.status === 3 ? 'good' : 'neutral'">{{ statuses[order.status] || '未知状态' }}</span></td>
            <td class="admin-date">{{ adminDate(order.createdTime) }}<span class="admin-cell-sub">{{ adminTime(order.createdTime) }}</span></td>
            <td><button v-if="order.status === 4" class="admin-btn danger small" :disabled="busy !== null" @click="deleteOrder(order)">{{ busy === order.id ? '处理中…' : '删除记录' }}</button><span v-else class="admin-cell-sub">—</span></td>
          </tr></tbody>
        </table></div>
      </AdminListState>
      <AdminPagination v-if="!error" :page="page" :page-size="pageSize" :total="total" :total-pages="totalPages" :loading="loading" @change="changePage" @resize="resizePage" />
    </section>
  </main>
</template>
