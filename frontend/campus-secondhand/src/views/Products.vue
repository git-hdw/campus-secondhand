<template>
  <div class="products-page">
    <main class="main-content workspace-page">
      <PageHeading title="商品列表" description="发现校园里的实惠好物" />

      <div class="search-section">
        <div class="search-bar-wrapper">
          <div class="search-bar">
            <svg class="search-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <line x1="21" y1="21" x2="16.65" y2="16.65"/>
            </svg>
            <input 
              type="text" 
              v-model="searchKeyword" 
              placeholder="搜索商品名称、描述..." aria-label="搜索商品"
              @keyup.enter="handleSearch"
              @focus="showSearchSuggestions = true"
              @blur="hideSearchSuggestions"
            />
            <button v-if="searchKeyword" class="clear-btn" aria-label="清除搜索" @click="clearSearch">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"/>
                <line x1="6" y1="6" x2="18" y2="18"/>
              </svg>
            </button>
            <button class="search-btn" @click="handleSearch">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              搜索
            </button>
          </div>
          
          <div v-if="showSearchSuggestions && (searchSuggestions.length > 0 || searchHistory.length > 0)" class="search-suggestions">
            <div v-if="searchHistory.length > 0" class="suggestion-section">
              <div class="suggestion-header">
                <span>搜索历史</span>
                <button class="clear-history-btn" @click="clearSearchHistory">清空</button>
              </div>
              <div class="suggestion-item" v-for="(item, index) in searchHistory.slice(0, 5)" :key="`history-${index}`" @mousedown="selectSuggestion(item)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
                <span>{{ item }}</span>
              </div>
            </div>
            
            <div v-if="searchSuggestions.length > 0" class="suggestion-section">
              <div class="suggestion-header">
                <span>搜索建议</span>
              </div>
              <div class="suggestion-item" v-for="(item, index) in searchSuggestions" :key="`suggestion-${index}`" @mousedown="selectSuggestion(item)">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"/>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"/>
                </svg>
                <span>{{ item }}</span>
              </div>
            </div>
          </div>
        </div>
        
        <div class="advanced-filters">
          <button class="filter-toggle-btn" :aria-expanded="showAdvancedFilters" @click="showAdvancedFilters = !showAdvancedFilters">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="4" y1="21" x2="4" y2="14"/>
              <line x1="4" y1="10" x2="4" y2="3"/>
              <line x1="12" y1="21" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12" y2="3"/>
              <line x1="20" y1="21" x2="20" y2="16"/>
              <line x1="20" y1="12" x2="20" y2="3"/>
            </svg>
            高级筛选
          </button>
          
          <div v-if="showAdvancedFilters" class="advanced-filter-options">
            <div class="filter-group">
              <label>价格范围</label>
              <div class="price-range">
                <input 
                  type="number" 
                  v-model="priceMin" 
                  placeholder="最低" 
                  class="price-input" 
                  @input="validatePriceRange"
                  min="0"
                  step="0.01"
                />
                <span>-</span>
                <input 
                  type="number" 
                  v-model="priceMax" 
                  placeholder="最高" 
                  class="price-input" 
                  @input="validatePriceRange"
                  min="0"
                  step="0.01"
                />
              </div>
              <p v-if="priceError" class="error-message">{{ priceError }}</p>
            </div>
            
            <div class="filter-group">
              <label>成色</label>
              <div class="condition-options">
                <button 
                  v-for="cond in conditions" 
                  :key="cond.value"
                  class="condition-btn"
                  :class="{ active: selectedCondition === cond.value }"
                  @click="selectedCondition = selectedCondition === cond.value ? '' : cond.value"
                >
                  {{ cond.label }}
                </button>
              </div>
            </div>
            
            <div class="filter-group">
              <label>排序方式</label>
              <div class="sort-options">
                <button 
                  v-for="sort in sortOptions" 
                  :key="sort.value"
                  class="sort-btn"
                  :class="{ active: selectedSort === sort.value }"
                  @click="selectedSort = sort.value"
                >
                  {{ sort.label }}
                </button>
              </div>
            </div>
            
            <div class="filter-actions">
              <button class="apply-filter-btn" @click="applyFilters">
                应用筛选
              </button>
              <button class="reset-filter-btn" @click="resetFilters">
                重置
              </button>
            </div>
          </div>
        </div>
      </div>

      <p v-if="priceError" class="inline-error" role="alert">{{ priceError }}</p>
      <div class="filter-section">
        <div class="filter-tabs">
          <button 
            class="filter-tab" :aria-pressed="activeCategory === 'all'"
            :class="{ active: activeCategory === 'all' }"
            @click="selectCategory('all')"
          >
            全部
          </button>
          <button 
            v-for="cat in categories" 
            :key="cat.value"
            class="filter-tab" 
            :class="{ active: activeCategory === cat.value }" :aria-pressed="activeCategory === cat.value"
            @click="selectCategory(cat.value)"
          >
            {{ cat.label }}
          </button>
        </div>
      </div>

      <div class="products-section">
        <div v-if="loading" class="loading-container">
          <div class="loading-spinner"></div>
          <p>加载中...</p>
        </div>
        
        <div v-else-if="error" class="error-container">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <line x1="12" y1="8" x2="12" y2="12"/>
            <line x1="12" y1="16" x2="12.01" y2="16"/>
          </svg>
          <p>{{ error }}</p>
          <button class="retry-btn" @click="fetchProducts">重新加载</button>
        </div>
        
        <div v-else-if="allProducts.length === 0" class="empty-container">
          <svg v-if="searchKeyword || activeCategory !== 'all' || priceMin || priceMax || selectedCondition" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="11" cy="11" r="8"/>
            <line x1="21" y1="21" x2="16.65" y2="16.65"/>
          </svg>
          <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="9" cy="21" r="1"/>
            <circle cx="20" cy="21" r="1"/>
            <path d="M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"/>
          </svg>
          <p v-if="searchKeyword">没有找到包含"{{ searchKeyword }}"的商品</p>
          <p v-else-if="activeCategory !== 'all'">该分类下暂无商品</p>
          <p v-else-if="priceMin || priceMax || selectedCondition">没有符合筛选条件的商品</p>
          <p v-else>暂无商品</p>
          <div class="empty-actions">
            <button v-if="searchKeyword || priceMin || priceMax || selectedCondition" class="clear-filters-btn" @click="clearAllFilters">
              清除所有筛选
            </button>
            <button class="post-btn" @click="go('/post')">发布商品</button>
          </div>
        </div>
        
        <div v-else class="products-grid catalog-grid">
          <div 
            v-for="product in allProducts"
            :key="product.id" 
            class="product-card catalog-card" role="link" tabindex="0" :aria-label="product.title + '，¥' + product.price" @keydown.enter="viewProduct(product.id)"
            @click="viewProduct(product.id)"
          >
            <div class="product-image catalog-picture">
              <img v-if="splitImages(product.images)[0] && !failedImages.has(product.id + ':' + product.images)" :src="splitImages(product.images)[0]" :alt="product.title" class="product-img" loading="lazy" @error="failedImages.add(product.id + ':' + product.images)" />
              <div v-else class="image-placeholder" role="img" aria-label="暂无图片">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" ry="2"/>
                  <circle cx="8.5" cy="8.5" r="1.5"/>
                  <polyline points="21 15 16 10 5 21"/>
                </svg>
                <span>暂无图片</span>
              </div>
              <div class="product-status catalog-status" :class="saleClass(product)">
                {{ saleText(product) }}
              </div>
            </div>
            <div class="product-info catalog-body">
              <h3 class="product-title catalog-title">{{ product.title }}</h3>
              <p class="product-price catalog-price"><span class="currency">¥</span>{{ formatPrice(product.price) }}</p>
              <div class="catalog-meta"><p class="catalog-location"><Location aria-hidden="true" />{{ product.tradePlace || '见面地点待商议' }}</p><span class="catalog-views">{{ product.viewCount || 0 }} 次浏览</span></div>
            </div>
          </div>
        </div>
        <div v-if="!loading && !error && total > pageSize" class="pager">
          <button type="button" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
          <span>第 {{ page }} / {{ pageCount }} 页，共 {{ total }} 件</span>
          <button type="button" :disabled="page >= pageCount" @click="goPage(page + 1)">下一页</button>
        </div>
      </div>
    </main>

    <SiteFooter />
  </div>
</template>

<script setup>
import PageHeading from '../components/PageHeading.vue'
import SiteFooter from '../components/SiteFooter.vue'
import { Location } from '@element-plus/icons-vue'
import { formatCatalogAmount as formatPrice } from '../utils/catalogFormat'
defineOptions({ name: 'ProductMarketplace' })
import { ref, computed, onMounted, onUnmounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProductList, splitImages } from '../api/product'
import { loadCategories } from '../constants/categories'

const router = useRouter()
const route = useRoute()
const allProducts = ref([])
const failedImages = ref(new Set())
const page = ref(1)
const pageSize = 12
const total = ref(0)
const pageCount = computed(() => Math.max(1, Math.ceil(total.value / pageSize)))
const loading = ref(true)
const error = ref('')
const ready = ref(false)
let fetchTimer = null
let requestVersion = 0
const searchKeyword = ref('')
const activeCategory = ref('all')
const showAdvancedFilters = ref(false)
const priceMin = ref('')
const priceMax = ref('')
const selectedCondition = ref('')
const selectedSort = ref('newest')
const showSearchSuggestions = ref(false)
const searchHistory = ref([])
const priceError = ref('')

const conditions = [
  { value: '全新', label: '全新' },
  { value: '几乎全新', label: '几乎全新' },
  { value: '轻微使用痕迹', label: '轻微使用痕迹' },
  { value: '明显使用痕迹', label: '明显使用痕迹' },
  { value: '有磨损', label: '有磨损' }
]

const sortOptions = [
  { value: 'newest', label: '最新发布' },
  { value: 'price-low', label: '价格从低到高' },
  { value: 'price-high', label: '价格从高到低' },
  { value: 'views', label: '浏览量最多' }
]

const categories = ref([])

const searchSuggestions = computed(() => [])

function go(path) {
  router.push(path)
}

function hideSearchSuggestions() {
  setTimeout(() => {
    showSearchSuggestions.value = false
  }, 200)
}

function selectSuggestion(suggestion) {
  searchKeyword.value = suggestion
  showSearchSuggestions.value = false
  handleSearch()
}

function clearSearch() {
  searchKeyword.value = ''
  showSearchSuggestions.value = false
}

function saveSearchHistory(keyword) {
  if (!keyword.trim()) return
  
  const index = searchHistory.value.indexOf(keyword)
  if (index > -1) {
    searchHistory.value.splice(index, 1)
  }
  
  searchHistory.value.unshift(keyword)
  searchHistory.value = searchHistory.value.slice(0, 10)
  
  localStorage.setItem('searchHistory', JSON.stringify(searchHistory.value))
}

function clearSearchHistory() {
  searchHistory.value = []
  localStorage.removeItem('searchHistory')
}

function loadSearchHistory() {
  const saved = localStorage.getItem('searchHistory')
  if (saved) {
    try {
      searchHistory.value = JSON.parse(saved)
    } catch (e) {
      console.error('Failed to parse search history:', e)
    }
  }
}

async function fetchProducts() {
  validatePriceRange()
  if (priceError.value) { loading.value = false; return }
  const version = ++requestVersion
  loading.value = true
  error.value = ''
  try {
    const res = await getProductList({
      page: page.value,
      pageSize,
      category: activeCategory.value === 'all' ? '' : activeCategory.value,
      keyword: searchKeyword.value.trim(),
      condition: selectedCondition.value,
      minPrice: priceMin.value,
      maxPrice: priceMax.value,
      sort: selectedSort.value
    })
    if (version !== requestVersion) return
    if (res.code === 200 && res.data) {
      allProducts.value = res.data.items || []
      total.value = Number(res.data.total || 0)
      page.value = Number(res.data.page || page.value)
    }
  } catch (e) {
    if (version === requestVersion) error.value = e.message || '加载商品失败'
  } finally {
    if (version === requestVersion) loading.value = false
  }
}

function scheduleFetch() {
  if (!ready.value) return
  requestVersion++
  loading.value = true
  page.value = 1
  clearTimeout(fetchTimer)
  fetchTimer = setTimeout(fetchProducts, 200)
}

function goPage(next) {
  if (next < 1 || next > pageCount.value || next === page.value) return
  clearTimeout(fetchTimer)
  page.value = next
  fetchProducts()
}

async function handleSearch() {
  saveSearchHistory(searchKeyword.value)
  showSearchSuggestions.value = false
  clearTimeout(fetchTimer)
  page.value = 1
  fetchProducts()
}

async function selectCategory(category) {
  activeCategory.value = category
  showAdvancedFilters.value = false
}

function applyFilters() {
  // 验证价格范围
  validatePriceRange()
  // 如果价格范围有效，才关闭筛选面板
  if (!priceError.value) {
    showAdvancedFilters.value = false
  }
}

function resetFilters() {
  priceMin.value = ''
  priceMax.value = ''
  priceError.value = ''
  selectedCondition.value = ''
  selectedSort.value = 'newest'
}

function validatePriceRange() {
  priceError.value = ''
  if ([priceMin.value, priceMax.value].some(value => value !== '' && (!Number.isFinite(Number(value)) || Number(value) < 0))) {
    priceError.value = '价格需要是大于或等于 0 的数字'
    return
  }
  if (priceMin.value && priceMax.value) {
    const min = parseFloat(priceMin.value)
    const max = parseFloat(priceMax.value)
    if (min > max) {
      priceError.value = '最低价格不能大于最高价格'
    }
  }
}

function clearAllFilters() {
  searchKeyword.value = ''
  activeCategory.value = 'all'
  priceMin.value = ''
  priceMax.value = ''
  priceError.value = ''
  selectedCondition.value = ''
  selectedSort.value = 'newest'
  showAdvancedFilters.value = false
}

function viewProduct(id) {
  router.push(`/products/${id}`)
}

function getStatusClass(status) {
  const classes = {
    0: 'status-available',
    1: 'status-sold',
    2: 'status-offline'
  }
  return classes[status] || 'status-available'
}

function getStatusText(status) {
  const texts = {
    0: '在售',
    1: '已售',
    2: '下架',
    3: '待审核'
  }
  return texts[status] || '在售'
}

function saleText(product) {
  if (product && product.status === 0 && product.pendingOrder) return '待见面'
  return getStatusText(product && product.status)
}

function saleClass(product) {
  if (product && product.status === 0 && product.pendingOrder) return 'status-pending'
  return getStatusClass(product && product.status)
}


onMounted(async () => {
  categories.value = await loadCategories()
  loadSearchHistory()
  
  if (route.query.search) {
    searchKeyword.value = route.query.search
  }
  
  if (route.query.category) {
    activeCategory.value = route.query.category
  }
  
  ready.value = true
  fetchProducts()
})

watch(
  () => [searchKeyword.value, activeCategory.value, selectedCondition.value, selectedSort.value, priceMin.value, priceMax.value],
  scheduleFetch
)

watch(
  () => route.fullPath,
  () => {
    const nextSearch = typeof route.query.search === 'string' ? route.query.search : ''
    const nextCategory = typeof route.query.category === 'string' ? route.query.category : 'all'
    if (nextSearch !== searchKeyword.value) searchKeyword.value = nextSearch
    if (nextCategory !== activeCategory.value) activeCategory.value = nextCategory
  }
)
onUnmounted(() => { clearTimeout(fetchTimer); requestVersion++ })
</script>

<style scoped>
.products-page {
  min-height: 100vh;
  background: var(--color-background);
  position: relative;
  overflow-x: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.page-header {
  text-align: center;
  margin-bottom: 40px;
}

.page-title {
  font-size: 36px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 8px;
}

.page-subtitle {
  font-size: 16px;
  color: var(--color-muted-foreground);
}

.search-section {
  margin-bottom: 30px;
}

.search-bar-wrapper {
  position: relative;
}

.search-bar {
  display: flex;
  align-items: center;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 6px 6px 6px 20px;
  backdrop-filter: blur(10px);
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.search-bar:focus-within {
  border-color: rgba(26, 26, 26, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.search-icon {
  width: 22px;
  height: 22px;
  color: var(--color-muted-foreground);
  margin-right: 12px;
}

.search-bar input {
  flex: 1;
  background: transparent;
  border: none;
  outline: none;
  font-size: 16px;
  color: var(--color-foreground);
  padding: 12px 0;
}

.search-bar input::placeholder {
  color: var(--color-muted-foreground);
}

.search-bar .search-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 24px;
  background: var(--color-brand);
  border: none;
  border-radius: 12px;
  color: var(--color-foreground);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.search-bar .search-btn svg {
  width: 18px;
  height: 18px;
}

.search-bar .search-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(26, 26, 26, 0.3);
}

.clear-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  background: rgba(255, 255, 255, 0.1);
  border: none;
  border-radius: 8px;
  color: var(--color-muted-foreground);
  cursor: pointer;
  margin-right: 8px;
  transition: all 0.3s ease;
}

.clear-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: var(--color-foreground);
}

.clear-btn svg {
  width: 18px;
  height: 18px;
}

.search-suggestions {
  position: absolute;
  top: 100%;
  left: 0;
  right: 0;
  margin-top: 8px;
  background: rgba(20, 20, 40, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 16px;
  padding: 12px 0;
  z-index: 1000;
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.4);
}

.suggestion-section {
  padding: 8px 0;
}

.suggestion-section + .suggestion-section {
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.suggestion-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 4px 16px 12px;
}

.suggestion-header span {
  font-size: 12px;
  font-weight: 600;
  color: var(--color-muted-foreground);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.clear-history-btn {
  font-size: 12px;
  color: rgba(239, 68, 68, 0.8);
  background: none;
  border: none;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: all 0.3s ease;
}

.clear-history-btn:hover {
  background: rgba(239, 68, 68, 0.1);
  color: #ef4444;
}

.suggestion-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 10px 16px;
  color: var(--color-muted-foreground);
  cursor: pointer;
  transition: all 0.2s ease;
}

.suggestion-item:hover {
  background: rgba(255, 255, 255, 0.08);
  color: var(--color-foreground);
}

.suggestion-item svg {
  width: 18px;
  height: 18px;
  flex-shrink: 0;
  opacity: 0.5;
}

.suggestion-item span {
  flex: 1;
  font-size: 14px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.advanced-filters {
  position: relative;
}

.filter-toggle-btn {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: var(--color-muted-foreground);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
  margin-bottom: 16px;
}

.filter-toggle-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  border-color: rgba(26, 26, 26, 0.5);
}

.filter-toggle-btn svg {
  width: 18px;
  height: 18px;
}

.advanced-filter-options {
  background: rgba(255, 255, 255, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 16px;
  padding: 24px;
  margin-bottom: 20px;
  backdrop-filter: blur(10px);
}

.filter-group {
  margin-bottom: 24px;
}

.filter-group:last-child {
  margin-bottom: 0;
}

.filter-group label {
  display: block;
  font-size: 14px;
  font-weight: 600;
  color: var(--color-foreground);
  margin-bottom: 12px;
}

.price-range {
  display: flex;
  align-items: center;
  gap: 12px;
}

.price-input {
  flex: 1;
  padding: 10px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: var(--color-foreground);
  font-size: 14px;
  outline: none;
  transition: all 0.3s ease;
}

.price-input:focus {
  border-color: rgba(26, 26, 26, 0.5);
  background: rgba(255, 255, 255, 0.12);
}

.price-input::placeholder {
  color: var(--color-muted-foreground);
}

.price-range span {
  color: var(--color-muted-foreground);
  font-size: 16px;
}

.error-message {
  color: #ef4444;
  font-size: 12px;
  margin-top: 8px;
  margin-left: 4px;
}

.condition-options,
.sort-options {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.condition-btn,
.sort-btn {
  padding: 8px 16px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 8px;
  color: var(--color-muted-foreground);
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.condition-btn:hover,
.sort-btn:hover {
  background: rgba(255, 255, 255, 0.12);
  color: var(--color-foreground);
}

.condition-btn.active,
.sort-btn.active {
  background: var(--color-brand);
  border-color: transparent;
  color: var(--color-foreground);
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid rgba(255, 255, 255, 0.1);
}

.apply-filter-btn,
.reset-filter-btn {
  flex: 1;
  padding: 12px 24px;
  border: none;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.apply-filter-btn {
  background: var(--color-brand);
  color: var(--color-foreground);
}

.apply-filter-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(26, 26, 26, 0.3);
}

.reset-filter-btn {
  background: rgba(255, 255, 255, 0.1);
  color: var(--color-muted-foreground);
  border: 1px solid rgba(255, 255, 255, 0.15);
}

.reset-filter-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  color: var(--color-foreground);
}

.filter-section {
  margin-bottom: 30px;
}

.filter-tabs {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-tab {
  padding: 10px 20px;
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.15);
  border-radius: 10px;
  color: var(--color-muted-foreground);
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
  transition: all 0.3s ease;
}

.filter-tab:hover {
  background: rgba(255, 255, 255, 0.15);
  color: var(--color-foreground);
}

.filter-tab.active {
  background: var(--color-brand);
  border-color: transparent;
  color: var(--color-foreground);
}

.products-section {
  min-height: 400px;
}

.loading-container,
.error-container,
.empty-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 80px 20px;
  color: var(--color-muted-foreground);
}

.loading-spinner {
  width: 50px;
  height: 50px;
  border: 3px solid rgba(255, 255, 255, 0.1);
  border-top-color: var(--color-foreground);
  border-radius: 50%;
  animation: spin 1s linear infinite;
  margin-bottom: 20px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.error-container svg,
.empty-container svg {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
  opacity: 0.5;
}

.retry-btn,
.post-btn {
  margin-top: 20px;
  padding: 12px 32px;
  background: var(--color-brand);
  border: none;
  border-radius: 12px;
  color: var(--color-foreground);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.retry-btn:hover,
.post-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(26, 26, 26, 0.3);
}

.empty-actions {
  display: flex;
  gap: 12px;
  margin-top: 20px;
}

.clear-filters-btn {
  padding: 12px 32px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  color: var(--color-muted-foreground);
  font-size: 15px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
}

.clear-filters-btn:hover {
  background: rgba(255, 255, 255, 0.15);
  border-color: rgba(255, 255, 255, 0.3);
  color: var(--color-foreground);
  transform: translateY(-2px);
}

.products-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
  gap: 24px;
}

.product-card {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.12);
  border-radius: 20px;
  overflow: hidden;
  cursor: pointer;
  transition: all 0.3s ease;
}

.product-card:hover {
  transform: translateY(-8px);
  border-color: rgba(26, 26, 26, 0.4);
  box-shadow: 0 20px 40px rgba(0, 0, 0, 0.3);
}

.product-image {
  position: relative;
  height: 200px;
  background: rgba(0, 0, 0, 0.3);
}

.product-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.image-placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
}

.image-placeholder svg {
  width: 64px;
  height: 64px;
  color: rgba(255, 255, 255, 0.3);
}

.product-status {
  position: absolute;
  top: 12px;
  right: 12px;
  padding: 6px 14px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 600;
}

.status-available {
  background: var(--color-brand);
  color: var(--color-foreground);
}

.status-sold {
  background: var(--color-brand);
  color: var(--color-foreground);
}

.status-offline {
  background: rgba(255, 255, 255, 0.2);
  color: var(--color-muted-foreground);
}

.product-info {
  padding: 20px;
}

.product-title {
  font-size: 17px;
  font-weight: 600;
  color: var(--color-foreground);
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-desc {
  font-size: 13px;
  color: var(--color-muted-foreground);
  margin-bottom: 12px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-meta {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

.product-category,
.product-condition {
  padding: 4px 10px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 6px;
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.product-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.product-price {
  display: flex;
  align-items: baseline;
  gap: 2px;
}

.price-symbol {
  font-size: 18px;
  font-weight: 700;
  color: var(--color-foreground);
}

.price-value {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-foreground);
}

.product-views {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

.pager {
  display: flex;
  justify-content: center;
  align-items: center;
  gap: 12px;
  margin-top: 24px;
}

.pager button {
  border: 1px solid #e5e7eb;
  background: #fff;
  border-radius: 8px;
  padding: 8px 14px;
  cursor: pointer;
}

.pager button:disabled {
  opacity: 0.4;
  cursor: default;
}
</style>
