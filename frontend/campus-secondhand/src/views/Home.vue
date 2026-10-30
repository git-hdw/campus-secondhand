<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Search, Plus, ArrowRight, Reading, Monitor, Bicycle, Headset, ShoppingBag, CoffeeCup, MoreFilled, Grid } from '@element-plus/icons-vue'
import RecentItems from '../components/RecentItems.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { getProductList, getHomeStats, splitImages } from '../api/product'
import { loadCategories, CATEGORY_FALLBACK } from '../constants/categories'

defineOptions({ name: 'HomeMarket' })
const router = useRouter()
const searchKeyword = ref('')
const categories = ref(CATEGORY_FALLBACK.slice())
const stats = ref(null)
const recentItems = ref([])
const productsLoading = ref(true)
const productsError = ref('')
const categoryIcons = { books: Reading, electronics: Monitor, transport: Bicycle, gaming: Headset, clothing: ShoppingBag, living: CoffeeCup, other: MoreFilled }
const categoryCounts = computed(() => Object.fromEntries((stats.value?.categoryCounts || []).map(row => [row.category, row.count])))
function handleSearch() {
  const keyword = searchKeyword.value.trim()
  if (!keyword) { router.push('/products'); return }
  try {
    const saved = JSON.parse(localStorage.getItem('homeSearchHistory') || '[]')
    const history = Array.isArray(saved) ? saved : []
    localStorage.setItem('homeSearchHistory', JSON.stringify([keyword, ...history.filter(item => item !== keyword)].slice(0, 10)))
  } catch { /* Search remains available when browser storage is unavailable. */ }
  router.push({ path: '/products', query: { search: keyword } })
}
async function loadProducts() {
  productsLoading.value = true
  productsError.value = ''
  try {
    const res = await getProductList({ page: 1, pageSize: 8, sort: 'newest' })
    if (res.code !== 200 || !Array.isArray(res.data?.items)) throw new Error(res.message || '商品加载失败，请重试')
    recentItems.value = res.data.items.map(item => ({
      id: item.id, title: item.title, price: item.price, place: item.tradePlace || '',
      pending: item.status === 0 && !!item.pendingOrder,
      img: splitImages(item.images)[0] || '',
    }))
  } catch (error) {
    productsError.value = error.message || '商品加载失败，请重试'
  } finally { productsLoading.value = false }
}
async function loadStats() {
  try {
    const res = await getHomeStats()
    if (res.code === 200 && res.data) stats.value = res.data
  } catch { stats.value = null }
}
onMounted(() => {
  loadCategories().then(list => { categories.value = list })
  loadProducts()
  loadStats()
})
</script>

<template>
  <main class="home-page market-home workspace-page">
    <section class="market-welcome" aria-labelledby="welcome-title">
      <div class="welcome-heading">
        <div>
          <h1 id="welcome-title">把闲置，留给需要的人</h1>
          <p>校内见面，先看实物再确认<span v-if="stats?.onSaleProducts != null" class="market-count">{{ stats.onSaleProducts }} 件闲置在等你</span></p>
        </div>
        <RouterLink to="/post" class="market-publish"><Plus aria-hidden="true" />发布闲置</RouterLink>
      </div>
      <form class="market-search" role="search" @submit.prevent="handleSearch">
        <Search aria-hidden="true" />
        <input v-model="searchKeyword" aria-label="搜索商品" placeholder="搜一搜教材、数码、生活好物…" />
        <button type="submit">搜索</button>
      </form>
    </section>

    <nav class="market-categories" aria-label="商品分类">
      <RouterLink to="/products" class="market-category all-category"><Grid aria-hidden="true" /><span>全部商品</span></RouterLink>
      <RouterLink v-for="cat in categories" :key="cat.value" :to="{ path: '/products', query: { category: cat.value } }" class="market-category">
        <component :is="categoryIcons[cat.value] || MoreFilled" aria-hidden="true" />
        <span>{{ cat.label }}</span><small v-if="categoryCounts[cat.value] != null">{{ categoryCounts[cat.value] }}</small>
      </RouterLink>
    </nav>

    <section class="market-products" aria-labelledby="latest-title" :aria-busy="productsLoading">
      <div class="market-section-heading">
        <h2 id="latest-title">最新发布</h2>
        <RouterLink to="/products" class="market-view-all">查看全部<ArrowRight aria-hidden="true" /></RouterLink>
      </div>
      <template v-if="productsLoading">
        <p class="visually-hidden" role="status">正在加载最新商品…</p>
        <div class="market-product-grid catalog-grid market-skeletons" aria-hidden="true">
          <div v-for="index in 8" :key="index" class="market-skeleton"><div class="skeleton-image" /><div class="skeleton-text" /><div class="skeleton-price" /></div>
        </div>
      </template>
      <div v-else-if="productsError" class="market-state" role="alert">
        <p>{{ productsError }}</p><button type="button" @click="loadProducts">重新加载</button>
      </div>
      <RecentItems v-else :items="recentItems" />
    </section>

    <section class="market-trade" aria-label="校园交易提示">
      <div class="market-trade-points"><span>发布后审核</span><span>校内见面验货</span><span>买家确认收货</span></div>
      <RouterLink to="/help#trade">了解交易流程<ArrowRight aria-hidden="true" /></RouterLink>
    </section>
    <SiteFooter />
  </main>
</template>

<style src="../styles/home-market.css"></style>
