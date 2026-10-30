<script setup>
import { computed, ref } from 'vue'
import axios from 'axios'
import { ElDialog } from 'element-plus'
import { Search, RefreshRight } from '@element-plus/icons-vue'
import AdminPagination from '../../components/AdminPagination.vue'
import AdminListState from '../../components/AdminListState.vue'
import { useAdminList, adminHeaders, requireAdminData, adminError } from '../../composables/useAdminList'
import { loadCategories, categoryLabel } from '../../constants/categories'
import { splitImages } from '../../api/product'
import { adminDate, adminTime, adminMoney } from '../../utils/adminFormat'
import { notify, confirmAction } from '../../utils/feedback'

const searchQuery = ref(''), categoryFilter = ref(''), statusFilter = ref('')
const categories = ref([])
loadCategories().then(rows => { categories.value = rows })
const busy = ref(null), selectedProduct = ref(null), showDetailModal = ref(false), detailLoading = ref(null)
const { items: products, total, page, pageSize, totalPages, loading, error, load, applyFilters, changePage, resizePage } =
  useAdminList('/api/admin/products', () => ({ search: searchQuery.value.trim(), category: categoryFilter.value, status: statusFilter.value }))
const detailImages = computed(() => splitImages(selectedProduct.value?.images))
const statusLabels = { 0: '在售', 1: '已成交', 2: '已下架', 3: '待审核' }
function saleText(product) { return product.status === 0 && product.pendingOrder ? '待见面' : statusLabels[product.status] || '未知状态' }
function saleClass(product) { return product.pendingOrder ? 'waiting' : ({ 0: 'good', 1: 'neutral', 2: 'neutral', 3: 'review' }[product.status] || 'neutral') }
function firstImage(product) { return splitImages(product.images)[0] || '/sample/phone.svg' }
function resetSearch() { searchQuery.value = ''; categoryFilter.value = ''; statusFilter.value = ''; applyFilters() }
function actionText(product) { return product.status === 3 ? '通过审核' : product.status === 2 ? '上架' : '下架' }

async function toggleStatus(product) {
  if (busy.value !== null || product.status === 1 || product.pendingOrder) return
  busy.value = product.id
  try {
    const action = actionText(product)
    if (!await confirmAction('确认' + action + '「' + product.title + '」吗？', action + '商品', '确认' + action)) return
    const status = product.status === 0 ? 2 : 0
    requireAdminData(await axios.put('/api/admin/products/' + product.id + '/status', status, { headers: { ...adminHeaders(), 'Content-Type': 'application/json' } }))
    notify('商品已' + (status === 2 ? '下架' : '上架'))
    await load()
  } catch (err) { notify(adminError(err)) }
  finally { busy.value = null }
}
async function viewProductDetail(product) {
  if (detailLoading.value !== null) return
  detailLoading.value = product.id
  try {
    const data = requireAdminData(await axios.get('/api/admin/products/' + product.id, { headers: adminHeaders() }))
    selectedProduct.value = { ...product, ...data, sellerName: data.sellerName || product.sellerName, pendingOrder: product.pendingOrder }
    showDetailModal.value = true
  } catch (err) { notify(adminError(err)) }
  finally { detailLoading.value = null }
}
</script>

<template>
  <main class="admin-products admin-page">
    <div class="admin-page-heading"><div><h1>商品管理</h1><p class="admin-page-description">审核新发布的闲置，维护校园商品信息。</p></div><span class="admin-total">共 {{ total }} 件商品</span></div>
    <section class="admin-panel">
      <form class="admin-toolbar" role="search" @submit.prevent="applyFilters">
        <div class="admin-search-field"><Search aria-hidden="true" /><input v-model="searchQuery" class="search-input" aria-label="搜索商品" placeholder="搜索商品标题或描述" /></div>
        <select v-model="categoryFilter" class="admin-select filter-select" aria-label="商品分类" @change="applyFilters"><option value="">全部分类</option><option v-for="category in categories" :key="category.value" :value="category.value">{{ category.label }}</option></select>
        <select v-model="statusFilter" class="admin-select filter-select" aria-label="商品状态" @change="applyFilters"><option value="">全部状态</option><option value="0">在售</option><option value="1">已成交</option><option value="2">已下架</option><option value="3">待审核</option></select>
        <button type="submit" class="admin-btn primary search-button">搜索</button><button type="button" class="admin-btn ghost" @click="resetSearch"><RefreshRight aria-hidden="true" />重置</button>
      </form>
      <div class="products-table-container">
        <AdminListState :loading="loading" :error="error" :empty="!products.length" empty-text="没有找到符合条件的商品" @retry="load">
          <div class="admin-table-scroll"><table class="admin-table">
            <thead><tr><th scope="col">商品信息</th><th scope="col">卖家</th><th scope="col">售价</th><th scope="col">分类</th><th scope="col">状态</th><th scope="col">发布时间</th><th scope="col">操作</th></tr></thead>
            <tbody><tr v-for="product in products" :key="product.id">
              <td><div class="admin-product-cell"><img class="admin-product-thumb" :src="firstImage(product)" :alt="product.title" /><div><span class="admin-cell-title">{{ product.title }}</span><span class="admin-cell-sub">商品 #{{ product.id }}</span></div></div></td>
              <td class="admin-nowrap">{{ product.sellerName || '#' + product.userId }}</td><td><span class="admin-price">¥{{ adminMoney(product.price) }}</span></td><td class="admin-nowrap">{{ categoryLabel(product.category) }}</td>
              <td><span class="admin-status" :class="saleClass(product)">{{ saleText(product) }}</span></td>
              <td class="admin-date">{{ adminDate(product.createdTime) }}<span class="admin-cell-sub">{{ adminTime(product.createdTime) }}</span></td>
              <td><div class="admin-row-actions"><button class="admin-btn small" :disabled="detailLoading !== null" @click="viewProductDetail(product)">{{ detailLoading === product.id ? '加载中…' : '详情' }}</button><button v-if="product.status !== 1 && !product.pendingOrder" class="admin-btn small" :class="product.status === 0 ? 'danger' : 'positive'" :disabled="busy !== null" @click="toggleStatus(product)">{{ busy === product.id ? '处理中…' : actionText(product) }}</button><span v-else-if="product.pendingOrder" class="admin-cell-sub">交易中</span></div></td>
            </tr></tbody>
          </table></div>
        </AdminListState>
      </div>
      <AdminPagination v-if="!error" :page="page" :page-size="pageSize" :total="total" :total-pages="totalPages" :loading="loading" @change="changePage" @resize="resizePage" />
    </section>
    <ElDialog v-model="showDetailModal" title="商品详情" width="720px" class="admin-detail-dialog" align-center>
      <template v-if="selectedProduct">
        <div v-if="detailImages.length" class="admin-detail-images"><img v-for="(src, index) in detailImages" :key="index" :src="src" :alt="selectedProduct.title + ' 图片 ' + (index + 1)" /></div>
        <h2 class="admin-detail-title">{{ selectedProduct.title }}</h2>
        <dl class="admin-detail-fields">
          <div><dt>售价</dt><dd class="admin-price">¥{{ adminMoney(selectedProduct.price) }}</dd></div><div><dt>状态</dt><dd>{{ saleText(selectedProduct) }}</dd></div>
          <div><dt>分类</dt><dd>{{ categoryLabel(selectedProduct.category) }}</dd></div><div><dt>成色</dt><dd>{{ selectedProduct.condition || '未填写' }}</dd></div>
          <div><dt>卖家</dt><dd>{{ selectedProduct.sellerName || '#' + selectedProduct.userId }}</dd></div><div><dt>见面地点</dt><dd>{{ selectedProduct.tradePlace || '双方协商' }}</dd></div>
          <div><dt>发布时间</dt><dd>{{ adminDate(selectedProduct.createdTime) }} {{ adminTime(selectedProduct.createdTime) }}</dd></div><div><dt>商品编号</dt><dd>#{{ selectedProduct.id }}</dd></div>
        </dl>
        <p class="admin-detail-description">{{ selectedProduct.description || '卖家未填写商品描述。' }}</p>
      </template>
      <template #footer><button class="admin-btn" @click="showDetailModal = false">关闭</button></template>
    </ElDialog>
  </main>
</template>
