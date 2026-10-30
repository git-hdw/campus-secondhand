<template>
  <div class="product-detail-page">
    <main class="main-content workspace-page">
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
        <button class="retry-btn" @click="fetchProduct">重新加载</button>
      </div>
      
      <div v-else-if="product" class="product-detail">
        <div class="product-gallery">
          <div class="main-image">
            <img v-if="!failedImages.has(currentImage) && !currentImage.includes('/sample/')" :src="currentImage" :alt="product.title" @error="failedImages.add(currentImage)" />
            <div v-else class="detail-image-placeholder" role="img" aria-label="暂无图片"><Picture aria-hidden="true" /><span>暂无图片</span></div>
          </div>
          <div v-if="productImages.length > 1" class="thumbnail-list">
            <button type="button"
              v-for="(img, idx) in productImages" 
              :key="idx"
              class="thumbnail"
              :class="{ active: currentImage === img }"
              :aria-label="`查看第 ${idx + 1} 张商品图片`"
              :aria-pressed="currentImage === img"
              @click="currentImage = img"
            >
              <img v-if="!failedImages.has(img)" :src="img" alt="" @error="failedImages.add(img)" />
              <Picture v-else aria-hidden="true" />
            </button>
          </div>
        </div>

        <div class="product-info-section">
          <div class="detail-heading">
            <h1 class="detail-title">{{ product.title }}</h1>
            <span class="product-status-badge" :class="saleClass(product)">{{ saleText(product) }}</span>
          </div>
          
          <div class="product-price">
            <span class="price-symbol">¥</span>
            <span class="price-value">{{ formatPrice(product.price) }}</span>
            <span v-if="Number(product.originalPrice) > Number(product.price)" class="original-price">原价 ¥{{ formatPrice(product.originalPrice) }}</span>
          </div>
          
          <div class="product-meta-row">
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 10c0 7-9 13-9 13s-9-6-9-13a9 9 0 0 1 18 0z"/>
                <circle cx="12" cy="10" r="3"/>
              </svg>
              {{ product.tradePlace || '见面地点未填写，请先问卖家' }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="4" width="18" height="18" rx="2" ry="2"/>
                <line x1="16" y1="2" x2="16" y2="6"/>
                <line x1="8" y1="2" x2="8" y2="6"/>
                <line x1="3" y1="10" x2="21" y2="10"/>
              </svg>
              {{ formatDate(product.createdTime) }}
            </span>
            <span class="meta-item">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"/>
                <line x1="21" y1="21" x2="16.65" y2="16.65"/>
              </svg>
              {{ product.viewCount || 0 }} 次浏览
            </span>
          </div>

          <div class="product-tags">
            <span class="tag">{{ getCategoryLabel(product.category) }}</span>
            <span class="tag">{{ getConditionLabel(product.condition) }}</span>
          </div>

          <div class="product-description-section">
            <h2 class="detail-section-title">商品描述</h2>
            <p class="product-description">{{ product.description || '暂无描述' }}</p>
          </div>

          <div class="seller-section">
            <h2 class="detail-section-title">卖家信息</h2>
            <div class="seller-card">
              <div class="seller-avatar">
                <img :src="seller.avatar || defaultAvatar" :alt="seller.username" />
              </div>
              <div class="seller-info">
                <div class="seller-name">{{ seller.username || '卖家' }}</div>
                <div class="seller-badges">
                  <span v-if="seller.student" class="seller-badge verified">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                      <polyline points="22 4 12 14.01 9 11.01"/>
                    </svg>
                    已登记学号
                  </span>
                  <span v-else class="seller-badge">未填学号</span>
                </div>
              </div>
              <button v-if="!isMine" class="contact-seller-btn" @click="contactSeller">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z"/>
                </svg>
                联系卖家
              </button>
            </div>
          </div>

          <div class="detail-actions">
          <p v-if="buyHint" class="buy-hint" :class="{ 'pending-hint': product.pendingOrder }">{{ buyHint }}</p>
          <div class="action-buttons">
            <button class="action-btn favorite" @click="toggleFavorite">
              <svg v-if="!isFavorited" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              <svg v-else viewBox="0 0 24 24" fill="currentColor" stroke="none">
                <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78l1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
              </svg>
              {{ isFavorited ? '已收藏' : '收藏' }}
            </button>
            <button v-if="product.status === 0 && !isMine && !product.pendingOrder" class="action-btn buy" type="button" @click="beginCheckout">
              立即购买
            </button>
          </div>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup>
import { notify } from '../utils/feedback'
import { Picture } from '@element-plus/icons-vue'
import { ref, computed, onMounted, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getProductDetail, splitImages, getMyFavorites, addFavorite, removeFavorite } from '../api/product'
import { getUserById } from '../api/user'
import { categoryLabel, loadCategories } from '../constants/categories'

const router = useRouter()
const route = useRoute()
const product = ref(null)
const seller = ref({})
const defaultAvatar = "data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 40 40'%3E%3Ccircle cx='20' cy='20' r='20' fill='%23334155'/%3E%3Ccircle cx='20' cy='16' r='6' fill='%2394a3b8'/%3E%3Cpath d='M8 36c0-6.6 5.4-12 12-12s12 5.4 12 12' fill='%2394a3b8'/%3E%3C/svg%3E"
const loading = ref(true)
const error = ref('')
const currentImage = ref('')
const failedImages = ref(new Set())
const isFavorited = ref(false)
const favoriteId = ref(null)

function storedUserId() {
  const raw = localStorage.getItem('user') || sessionStorage.getItem('user')
  if (!raw) return 0
  try {
    return Number(JSON.parse(raw).id) || 0
  } catch {
    return 0
  }
}

const isMine = computed(() => {
  const mine = storedUserId()
  return mine > 0 && Number(product.value?.userId) === mine
})

const productImages = computed(() => {
  const images = splitImages(product.value?.images)
  return images.length > 0 ? images : ['/sample/phone.svg']
})

async function fetchProduct() {
  loading.value = true
  error.value = ''
  try {
    const res = await getProductDetail(route.params.id)
    if (res.code === 200 && res.data) {
      product.value = res.data
      currentImage.value = productImages.value[0]
      rememberView(product.value)
      
      if (product.value.userId) {
        await fetchSellerInfo(product.value.userId)
      }
      await loadFavorite()
    }
  } catch (e) {
    error.value = e.message || '加载商品详情失败'
  } finally {
    loading.value = false
  }
}

async function fetchSellerInfo(userId) {
  try {
    const res = await getUserById(userId)
    if (res.code === 200 && res.data) {
      seller.value = res.data
    }
  } catch (e) {
    console.error('Failed to fetch seller info:', e)
  }
}

async function loadFavorite() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) return
  try {
    const res = await getMyFavorites()
    const row = (res.data || []).find(item => String(item.productId) === String(product.value.id))
    isFavorited.value = !!row
    favoriteId.value = row ? row.id : null
  } catch {
    isFavorited.value = false
  }
}

async function toggleFavorite() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    notify('请先登录后再收藏')
    goLogin(`/products/${product.value.id}`)
    return
  }
  try {
    if (isFavorited.value && favoriteId.value) {
      await removeFavorite(favoriteId.value)
      isFavorited.value = false
      favoriteId.value = null
    } else {
      const res = await addFavorite(product.value.id)
      isFavorited.value = true
      favoriteId.value = res.data?.id || null
    }
  } catch (e) {
    notify(e.message || '收藏失败')
  }
}

function rememberView(p) {
  const key = 'browseHistory'
  let list = []
  try {
    list = JSON.parse(localStorage.getItem(key) || '[]')
  } catch {
    list = []
  }
  if (!Array.isArray(list)) list = []
  list = list.filter(item => Number(item.id) !== Number(p.id))
  const when = new Date()
  const time = `${when.getMonth() + 1}月${when.getDate()}日 ${String(when.getHours()).padStart(2, '0')}:${String(when.getMinutes()).padStart(2, '0')}`
  list.unshift({
    id: p.id,
    title: p.title,
    price: `¥${Number(p.price || 0).toFixed(2)}`,
    image: splitImages(p.images)[0] || '/sample/phone.svg',
    time
  })
  localStorage.setItem(key, JSON.stringify(list.slice(0, 20)))
}

function goLogin(redirect) {
  router.push({ path: '/login', query: { redirect } })
}

function contactSeller() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token')
  if (!token) {
    notify('请先登录后再联系卖家')
    const q = new URLSearchParams({
      seller: seller.value.username || '',
      sellerId: String(product.value.userId || ''),
      productId: String(product.value.id),
      productTitle: product.value.title || ''
    })
    goLogin(`/messages?${q}`)
    return
  }
  if (isMine.value) {
    notify('不能咨询自己的商品')
    return
  }
  if (!seller.value.username) {
    notify('获取卖家信息失败，请稍后重试')
    return
  }
  
  router.push({
    path: '/messages',
    query: {
      seller: seller.value.username,
      sellerId: product.value.userId,
      productId: product.value.id,
      productTitle: product.value.title
    }
  })
}

const buyHint = computed(() => {
  if (!product.value || product.value.status !== 0) return ''
  if (isMine.value) return '这是你发布的商品，不能购买'
  if (product.value.pendingOrder) return '已有买家约定见面，暂时不能购买。商品尚未成交，可联系卖家了解情况。'
  return ''
})

function beginCheckout() {
  router.push(`/checkout/${product.value.id}`)
}

function formatPrice(price) {
  if (!price) return '0'
  return parseFloat(price).toFixed(2)
}

function formatDate(timestamp) {
  if (!timestamp) return '未知时间'
  const date = new Date(timestamp)
  const now = new Date()
  const diff = now - date
  const minutes = Math.floor(diff / 60000)
  const hours = Math.floor(diff / 3600000)
  const days = Math.floor(diff / 86400000)
  
  if (minutes < 1) return '刚刚发布'
  if (minutes < 60) return `${minutes}分钟前发布`
  if (hours < 24) return `${hours}小时前发布`
  if (days < 7) return `${days}天前发布`
  return date.toLocaleDateString('zh-CN')
}

function getCategoryLabel(category) {
  return categoryLabel(category)
}

function getConditionLabel(condition) {
  return condition || '未标明'
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

onMounted(() => {
  loadCategories()
  fetchProduct()
})
watch(() => route.params.id, fetchProduct)
</script>

<style scoped>
.product-detail-page { min-height: 100vh; background: var(--color-background); }
.product-detail { display: grid; grid-template-columns: minmax(0, min(40%, 440px)) minmax(0, 1fr); align-items: start; gap: 24px; padding: 24px; background: var(--color-card); border: 1px solid var(--ui-border); border-radius: var(--ui-radius-panel); }
.product-gallery { min-width: 0; }
.main-image { width: 100%; aspect-ratio: 4 / 3; background: #f4f4f2; border-radius: 8px; overflow: hidden; }
.main-image > img { display: block; width: 100%; height: 100%; object-fit: contain; }
.detail-image-placeholder { height: 100%; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 8px; font-size: 13px; color: var(--ui-muted) !important; }
.detail-image-placeholder > svg { width: 40px; height: 40px; color: #999 !important; }
.thumbnail-list { display: flex; gap: 8px; overflow-x: auto; padding: 4px; margin: 8px -4px -4px; }
.thumbnail { display: grid; place-items: center; flex: 0 0 56px; width: 56px; height: 56px; padding: 2px; border: 1px solid var(--ui-border); border-radius: 6px; background: #f4f4f2; cursor: pointer; }
.thumbnail.active { border: 2px solid #b5a300; padding: 1px; background: #fffbe1; }
.thumbnail img { width: 100%; height: 100%; object-fit: cover; border-radius: 3px; }
.thumbnail svg { width: 24px; height: 24px; color: #999 !important; }
.product-info-section { min-width: 0; display: flex; flex-direction: column; gap: 16px; }
.detail-heading { display: flex; align-items: flex-start; gap: 12px; }
.detail-title { flex: 1; min-width: 0; font-size: 26px; font-weight: 700; line-height: 1.4; margin: 0; overflow-wrap: anywhere; }
.product-status-badge { flex: none; margin-top: 4px; padding: 4px 10px; border-radius: 6px; font-size: 12px; line-height: 1.5; font-weight: 600; white-space: nowrap; }
.product-price { display: flex; flex-wrap: wrap; align-items: baseline; gap: 3px; margin: 0; }
.product-price .price-symbol { font-size: 18px; font-weight: 700; color: var(--color-accent) !important; }
.product-price .price-value { font-size: 32px; font-weight: 700; line-height: 1.2; color: var(--color-accent) !important; }
.product-price .original-price { margin-left: 10px; font-size: 13px; font-weight: 400; color: var(--ui-muted) !important; text-decoration: line-through; }
.product-meta-row { display: flex; flex-wrap: wrap; align-items: center; gap: 8px 20px; }
.meta-item { display: inline-flex; align-items: center; gap: 5px; font-size: 12px; color: var(--ui-muted) !important; overflow-wrap: anywhere; }
.meta-item svg { width: 15px; height: 15px; flex: none; color: var(--ui-muted) !important; }
.product-tags { display: flex; flex-wrap: wrap; gap: 8px; }
.tag { padding: 4px 9px; border: 1px solid #ebebeb; border-radius: 6px; background: #f7f7f7; font-size: 12px; color: #666 !important; }
.product-description-section, .seller-section { border-top: 1px solid #ededed; padding-top: 16px; }
.detail-section-title { font-size: 15px; font-weight: 600; line-height: 1.5; margin: 0 0 8px; }
.product-description { font-size: 14px; line-height: 1.8; white-space: pre-wrap; overflow-wrap: anywhere; color: #555 !important; margin: 0; }
.seller-card { display: flex; align-items: center; gap: 10px; background: transparent !important; }
.seller-avatar { flex: none; width: 40px; height: 40px; border-radius: 50%; overflow: hidden; background: #eee; }
.seller-avatar img { width: 100%; height: 100%; object-fit: cover; }
.seller-info { flex: 1; min-width: 0; }
.seller-name { font-size: 14px; font-weight: 600; overflow-wrap: anywhere; }
.seller-badges { margin-top: 2px; }
.seller-badge { display: inline-flex; align-items: center; gap: 4px; font-size: 11px; color: var(--ui-muted) !important; }
.seller-badge svg { width: 12px; height: 12px; color: var(--ui-muted) !important; }
#page-content .contact-seller-btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; flex: none; min-height: 36px; padding: 8px 12px; background: white; border: 1px solid var(--ui-border); border-radius: 8px; font-size: 13px; font-weight: 500; cursor: pointer; }
.contact-seller-btn svg { width: 16px; height: 16px; }
.detail-actions { border-top: 1px solid #ededed; padding-top: 16px; }
.buy-hint { margin: 0 0 12px; padding: 10px 12px; border-radius: 8px; background: #f6f6f6; color: #666 !important; font-size: 13px; line-height: 1.7; }
.buy-hint.pending-hint { background: #fff5e5; color: #875400 !important; }
.action-buttons { display: flex; gap: 12px; }
#page-content .action-buttons .action-btn { display: inline-flex; align-items: center; justify-content: center; gap: 6px; height: 42px; min-height: 42px; line-height: 20px; padding: 10px 24px; border-radius: 8px; border: 1px solid var(--ui-border); background: white; font-size: 14px; font-weight: 600; cursor: pointer; }
.action-btn svg { width: 18px; height: 18px; }
.action-buttons .favorite { min-width: 112px; }
#page-content .action-buttons .buy { flex: 1; background: var(--color-brand); border-color: var(--color-brand); }
.loading-container, .error-container { display: grid; justify-items: center; gap: 16px; padding: 64px 24px; border: 1px solid var(--ui-border); border-radius: 12px; background: white; text-align: center; font-size: 14px; }
.loading-container p, .error-container p { margin: 0; }
.error-container > svg { width: 32px; height: 32px; }
.loading-spinner { width: 28px; height: 28px; border: 2px solid #e5e5e5; border-top-color: #555; border-radius: 50%; animation: detail-spin .8s linear infinite; }
.retry-btn { border: 0; padding: 10px 18px; background: var(--color-brand); border-radius: 8px; cursor: pointer; }
@keyframes detail-spin { to { transform: rotate(360deg); } }
@media (prefers-reduced-motion: reduce) { .loading-spinner { animation: none; } }
@media (max-width: 900px) { .product-detail { grid-template-columns: minmax(0, 1fr); padding: 20px; } .product-gallery { max-width: 440px; width: 100%; } }
</style>
