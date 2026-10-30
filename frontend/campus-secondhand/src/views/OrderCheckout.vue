<script setup>
import { computed, onUnmounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getProductDetail, splitImages } from '../api/product'
import { createOrder, getOrderDetail, getOrderList } from '../api/order'
import { getUserById } from '../api/user'
import { readUser } from '../utils/session'

defineOptions({ name: 'OrderCheckoutPage' })
const route = useRoute(), router = useRouter()
const currentUserId = Number(readUser().id) || 0
const loading = ref(true), loadError = ref(''), submitError = ref(''), submitting = ref(false)
const product = ref(null), order = ref(null), contact = ref({}), existingOrder = ref(null)
let loadVersion = 0, stopped = false
const resultPage = computed(() => route.name === 'OrderResult')
const image = computed(() => splitImages(product.value?.images)[0] || '')
const isBuyer = computed(() => Number(order.value?.buyerId) === currentUserId)
const contactId = computed(() => resultPage.value
  ? (isBuyer.value ? order.value?.sellerId : order.value?.buyerId) : product.value?.userId)
const contactLabel = computed(() => resultPage.value && !isBuyer.value ? '买家' : '卖家')
const amount = computed(() => resultPage.value ? order.value?.price : product.value?.price)
const blockedReason = computed(() => {
  if (!product.value) return '商品信息暂不可用'
  if (Number(product.value.userId) === currentUserId) return '这是你发布的商品，不能购买自己的商品。'
  if (Number(product.value.status) !== 0) return '这件商品当前不在售，暂时无法下单。'
  if (product.value.pendingOrder) return '这件商品已有待见面订单，暂时不能重复下单。'
  return ''
})
const resultState = computed(() => {
  const status = Number(order.value?.status)
  if (status === 3) return { title: '这笔交易已完成', label: '已完成', text: '订单已完成，可以在我的订单中查看交易记录。', icon: 'check' }
  if (status === 4) return { title: '订单已取消', label: '已取消', text: '这笔订单已取消，可以继续浏览其他校园好物。', icon: 'cancel' }
  if (status === 0) return { title: isBuyer.value ? '下单成功，等待见面' : '订单已创建，等待见面', label: '待见面',
    text: `请先联系${contactLabel.value}约好时间和地点，见面验货后再完成交易。`, icon: 'check' }
  return { title: '订单详情', label: '订单状态待确认', text: '请在我的订单中查看当前状态。', icon: 'neutral' }
})

function money(value) {
  const number = Number(value)
  return Number.isFinite(number) ? number.toFixed(2) : '--'
}
function dateTime(value) {
  return value ? new Date(value).toLocaleString('zh-CN', { hour12: false }) : '—'
}
async function findExistingOrder(productId) {
  const response = await getOrderList({ productId, status: 0 })
  return (response.data?.items || response.data || []).find(item => Number(item.productId) === Number(productId) &&
    Number(item.buyerId) === currentUserId && Number(item.status) === 0)
}
function showResult(id) { return router.replace({ name: 'OrderResult', params: { orderId: String(id) } }) }

async function load() {
  const version = ++loadVersion
  loading.value = true
  loadError.value = ''; submitError.value = ''
  product.value = null; order.value = null; contact.value = {}; existingOrder.value = null
  try {
    if (resultPage.value) {
      const response = await getOrderDetail(route.params.orderId)
      if (stopped || version !== loadVersion) return
      if (!response.data?.id) throw new Error('订单信息暂不可用，请重试')
      order.value = response.data
      const details = await Promise.allSettled([
        order.value.snapshotJson || !order.value.productId ? Promise.resolve(null) : getProductDetail(order.value.productId), getUserById(contactId.value),
      ])
      if (stopped || version !== loadVersion) return
      if (order.value.snapshotJson) product.value = JSON.parse(order.value.snapshotJson)
      else if (details[0].status === 'fulfilled') product.value = details[0].value?.data || null
      if (details[1].status === 'fulfilled') contact.value = details[1].value.data || {}
    } else {
      const response = await getProductDetail(route.params.productId)
      if (stopped || version !== loadVersion) return
      if (!response.data?.id) throw new Error('商品信息暂不可用，请重试')
      product.value = response.data
      const details = await Promise.allSettled([
        getUserById(product.value.userId),
        product.value.pendingOrder ? findExistingOrder(product.value.id) : Promise.resolve(null),
      ])
      if (stopped || version !== loadVersion) return
      if (details[0].status === 'fulfilled') contact.value = details[0].value.data || {}
      if (details[1].status === 'fulfilled') existingOrder.value = details[1].value
    }
  } catch (error) {
    if (!stopped && version === loadVersion) loadError.value = error.message || '加载失败，请重试'
  } finally { if (!stopped && version === loadVersion) loading.value = false }
}

async function submitOrder() {
  if (submitting.value || !product.value || blockedReason.value) return
  submitting.value = true
  submitError.value = ''
  const productId = product.value.id, shownPrice = Number(product.value.price), version = loadVersion
  let requestSent = false
  try {
    const response = await getProductDetail(productId)
    if (stopped || version !== loadVersion) return
    product.value = response.data
    if (blockedReason.value) {
      existingOrder.value = await findExistingOrder(productId)
      if (stopped || version !== loadVersion) return
      if (existingOrder.value) await showResult(existingOrder.value.id)
      else submitError.value = blockedReason.value
      return
    }
    if (Number(product.value.price) !== shownPrice) {
      submitError.value = '商品价格已更新，请核对新的金额后再次确认下单。'
      return
    }
    requestSent = true
    const created = await createOrder(productId)
    if (stopped || version !== loadVersion) return
    if (!created.data?.id) throw new Error('下单结果暂未确认，正在核对订单')
    await showResult(created.data.id)
  } catch (error) {
    if (stopped || version !== loadVersion) return
    if (requestSent) {
      try {
        const found = await findExistingOrder(productId)
        if (stopped || version !== loadVersion) return
        if (found) { await showResult(found.id); return }
      } catch { /* Keep the original error visible when the result cannot be verified. */ }
    }
    submitError.value = error.message || '暂时无法下单，请稍后重试'
  } finally { submitting.value = false }
}

function contactOther() {
  if (!contactId.value) return
  router.push({ path: '/messages', query: { sellerId: contactId.value, seller: contact.value.username || '',
    productId: product.value?.id || order.value?.productId, productTitle: product.value?.title || '咨询的商品' } })
}
function backToProduct() { router.push(`/products/${product.value?.id || route.params.productId}`) }
watch(() => [route.name, route.params.productId, route.params.orderId], load, { immediate: true })
onUnmounted(() => { stopped = true; loadVersion++ })
</script>

<template>
  <main class="order-flow-page">
    <nav class="flow-breadcrumb" aria-label="页面位置"><RouterLink to="/products">商品</RouterLink><span>/</span><span>{{ resultPage ? '订单详情' : '确认订单' }}</span></nav>
    <ol v-if="!resultPage || !order || Number(order.status) === 0" class="flow-steps" aria-label="下单流程">
      <li class="done"><span>✓</span>挑选商品</li><li :class="resultPage ? 'done' : 'current'" :aria-current="!resultPage ? 'step' : undefined"><span>{{ resultPage ? '✓' : '2' }}</span>确认订单</li><li :class="{ current: resultPage }" :aria-current="resultPage ? 'step' : undefined"><span>3</span>联系卖家</li>
    </ol>

    <div v-if="loading" class="flow-state" role="status">{{ resultPage ? '正在加载订单结果…' : '正在核对商品信息…' }}</div>
    <div v-else-if="loadError" class="flow-state flow-error" role="alert"><h1>{{ resultPage ? '暂时无法查看订单' : '暂时无法加载商品' }}</h1><p>{{ loadError }}</p><button type="button" class="flow-primary" @click="load">重新加载</button><RouterLink to="/profile?tab=orders">查看我的订单</RouterLink></div>

    <template v-else-if="resultPage && order">
      <section class="order-result">
        <div class="result-icon" :class="{ cancelled: Number(order.status) === 4 }" aria-hidden="true"><svg v-if="resultState.icon === 'check'" viewBox="0 0 32 32" fill="none" stroke="currentColor" stroke-width="2.3"><path d="m8 16 5 5 11-11"/></svg><span v-else>{{ resultState.icon === 'cancel' ? '×' : 'i' }}</span></div>
        <h1>{{ resultState.title }}</h1><p class="result-subtitle">{{ resultState.text }}</p>
        <div class="result-body" :class="{ 'has-next-steps': Number(order.status) === 0 }"><div class="result-order-summary">
          <div class="result-product"><img v-if="image" :src="image" :alt="product?.title || '商品图片'" /><div v-else class="product-image-fallback" aria-hidden="true">▣</div><div><span class="flow-caption">{{ resultState.label }}</span><h2>{{ product?.title || '商品信息暂不可用' }}</h2><p>{{ contactLabel }}：{{ contact.username || '商品联系人' }}</p></div><strong class="result-price">¥{{ money(order.price) }}</strong></div>
          <dl class="result-order-meta"><div><dt>订单编号</dt><dd>{{ order.orderNo || order.id }}</dd></div><div><dt>下单时间</dt><dd>{{ dateTime(order.createdTime) }}</dd></div><div><dt>见面地点</dt><dd>{{ product?.tradePlace || '请与对方协商确定' }}</dd></div><div><dt>交易方式</dt><dd>校内见面，当面验货</dd></div></dl>
        </div>
        <div v-if="Number(order.status) === 0" class="next-steps"><h2>接下来这样做</h2><ol><li><span>1</span><div><strong>先联系{{ contactLabel }}</strong><p>确认商品细节，约好时间和地点。</p></div></li><li><span>2</span><div><strong>见面查看商品</strong><p>当面验货，双方确认后完成交易。</p></div></li><li><span>3</span><div><strong>收货后确认完成</strong><p>买家收下商品后，在我的订单中确认。</p></div></li></ol></div>
        </div><p class="offline-note">平台不收款。商品金额用于订单记录，实际交易在见面时由双方协商完成。</p>
        <div class="result-actions"><button v-if="Number(order.status) !== 4" type="button" class="flow-primary" @click="contactOther">联系{{ contactLabel }}，约好见面 →</button><RouterLink class="flow-secondary" to="/profile?tab=orders">查看我的订单</RouterLink></div>
        <RouterLink class="continue-browsing" to="/products">继续逛逛</RouterLink>
      </section>
    </template>

    <template v-else-if="product">
      <header class="checkout-heading"><span class="flow-caption">校内见面交易</span><h1>确认这件好物</h1><p>核对商品信息，下单后联系卖家约好见面。</p></header>
      <div class="checkout-grid">
        <div class="checkout-details">
          <section class="checkout-section"><h2>商品信息</h2><div class="checkout-product"><img v-if="image" :src="image" :alt="product.title" /><div v-else class="product-image-fallback" aria-hidden="true">▣</div><div><h3>{{ product.title }}</h3><p>{{ product.condition || '成色请咨询卖家' }}</p><p>卖家：{{ contact.username || '商品卖家' }}</p><span class="checkout-quantity">数量 1 件</span></div></div></section>
          <section class="checkout-section"><h2>见面安排</h2><dl class="meeting-info"><div><dt>参考地点</dt><dd>{{ product.tradePlace || '请与卖家协商确定' }}</dd></div><div><dt>见面时间</dt><dd>下单后通过私信与卖家约定</dd></div></dl><button type="button" class="ask-seller" @click="contactOther">先问问卖家 ↗</button></section>
          <section class="checkout-notice"><h2>下单前请了解</h2><p>下单后商品会保留为“待见面”，其他同学暂时无法购买。请先与卖家约好见面，查看并收下商品后，再到我的订单中确认完成。</p><p>如需变更计划，可在我的订单中取消。</p></section>
        </div>
        <aside class="checkout-total" aria-label="订单金额确认"><h2>订单确认</h2><div class="amount-row"><span>商品金额</span><span>¥{{ money(amount) }}</span></div><div class="amount-total"><span>本次商品金额</span><strong>¥{{ money(amount) }}</strong></div><p class="offline-note">平台不收款，见面验货后由双方完成交易。</p>
          <div v-if="blockedReason" class="checkout-warning" role="status">{{ blockedReason }}</div><p v-if="submitError" class="checkout-submit-error" role="alert">{{ submitError }}</p>
          <button v-if="existingOrder" type="button" class="flow-primary" @click="showResult(existingOrder.id)">查看已创建订单 →</button><button v-else type="button" class="flow-primary" :disabled="!!blockedReason || submitting" @click="submitOrder">{{ submitting ? '正在创建订单…' : '确认下单' }}</button><button type="button" class="back-to-product" :disabled="submitting" @click="backToProduct">返回商品，再看看</button>
        </aside>
      </div>
    </template>
  </main>
</template>

<style scoped>
.order-flow-page { max-width: 1040px; margin: auto; padding: 28px 28px 60px; }
.flow-breadcrumb { display: flex; align-items: center; gap: 10px; font-size: 12px; color: #77776f; margin-bottom: 20px; }
.flow-breadcrumb a { color: #77776f; text-decoration: none; }
.flow-steps { display: flex; justify-content: center; gap: 72px; padding: 0; margin: 0 0 24px; list-style: none; }
.flow-steps li { display: flex; gap: 9px; align-items: center; position: relative; color: #99998e; font-size: 13px; }
.flow-steps li + li::before { content: ''; position: absolute; right: calc(100% + 17px); width: 40px; height: 1px; background: #deded5; }
.flow-steps li > span { display: grid; place-items: center; width: 25px; height: 25px; border-radius: 50%; background: #e9e9e1; font-size: 12px; }
.flow-steps li.current { color: #222; font-weight: 650; }
.flow-steps li.current > span { background: var(--color-brand); }
.flow-steps li.done { color: #55564b; }
.flow-steps li.done > span { background: #e3e4d7; }
.checkout-heading { margin-bottom: 24px; }
.flow-caption { color: #838474; font-size: 11px; }
.checkout-heading h1 { font-size: 29px; margin: 7px 0; letter-spacing: -.5px; }
.checkout-heading p { margin: 0; font-size: 13px; color: #74756a; }
.checkout-grid { display: grid; grid-template-columns: minmax(0, 1fr) 315px; gap: 24px; align-items: start; }
.checkout-details { display: flex; flex-direction: column; gap: 16px; }
.checkout-section, .checkout-total { background: white; border: 1px solid #e7e7df; border-radius: 14px; padding: 24px; }
.checkout-section h2, .checkout-total h2 { margin: 0 0 21px; font-size: 16px; }
.checkout-product { display: flex; gap: 18px; align-items: center; }
.checkout-product > img, .checkout-product > .product-image-fallback { flex: none; width: 100px; height: 100px; border-radius: 10px; object-fit: cover; }
.checkout-product h3 { font-size: 16px; line-height: 1.5; margin: 0 0 7px; }
.checkout-product p { margin: 4px 0; font-size: 12px; color: #77786e; }
.checkout-quantity { display: inline-block; margin-top: 7px; font-size: 11px; color: #8b8c7f; }
.product-image-fallback { display: grid; place-items: center; background: #eeeee6; color: #99998c; font-size: 32px; }
.meeting-info { margin: 0; }
.meeting-info > div { display: flex; gap: 20px; margin-bottom: 12px; font-size: 13px; line-height: 1.6; }
.meeting-info dt { color: #86877b; flex: none; }
.meeting-info dd { margin: 0; }
.ask-seller { margin-top: 2px; padding: 0; border: 0; background: transparent; text-decoration: underline; text-underline-offset: 4px; font-size: 12px; }
.checkout-notice { padding: 4px 5px; }
.checkout-notice h2 { margin: 0 0 8px; font-size: 13px; font-weight: 650; }
.checkout-notice p { margin: 5px 0; font-size: 12px; line-height: 1.85; color: #7c7d71; }
.checkout-total { position: sticky; top: 100px; }
.amount-row { display: flex; justify-content: space-between; font-size: 13px; padding-bottom: 22px; border-bottom: 1px solid #eeeee6; }
.amount-total { display: flex; justify-content: space-between; align-items: baseline; gap: 10px; margin: 22px 0 12px; font-size: 12px; }
.amount-total strong { color: var(--color-accent); font-size: 29px; font-weight: 700; letter-spacing: -1px; }
.offline-note { font-size: 11px; color: #898a7d; line-height: 1.8; margin: 10px 0 22px; }
.flow-primary, .flow-secondary { display: inline-flex; align-items: center; justify-content: center; min-height: 46px; padding: 11px 22px; border: 1px solid transparent; border-radius: 10px; text-decoration: none; color: #222; font-size: 14px; font-weight: 600; background: var(--color-brand); }
.checkout-total .flow-primary { width: 100%; }
.back-to-product { display: block; width: 100%; padding: 12px 0 0; border: 0; background: transparent; font-size: 12px; color: #7b7c70; }
.checkout-warning, .checkout-submit-error { background: #fff7da; color: #93632b; padding: 11px 13px; font-size: 12px; line-height: 1.7; border-radius: 8px; margin: 0 0 15px; }
.checkout-submit-error { background: #fff0eb; color: #ab4328; }
.flow-state { padding: 72px 28px; border: 1px solid #e7e7df; border-radius: 16px; background: white; text-align: center; color: #6d6e62; font-size: 14px; }
.flow-state h1 { font-size: 22px; color: #222; }
.flow-state p { margin: 16px 0 26px; }
.flow-state > a { display: block; margin-top: 20px; font-size: 12px; color: #77786b; }
.order-result { max-width: 900px; margin: auto; background: white; border: 1px solid #e7e7df; border-radius: 18px; padding: 28px 32px 24px; text-align: center; }
.result-icon { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 12px; border-radius: 50%; background: #fff0a0; }
.result-icon svg { width: 31px; height: 31px; }
.result-icon.cancelled { background: #eeeeea; color: #838478; }
.result-icon span { font-size: 31px; }
.order-result > h1 { margin: 0 0 9px; font-size: 27px; letter-spacing: -.4px; }
.result-subtitle { margin: 0 0 20px; font-size: 12px; color: #818274; line-height: 1.8; }
.result-body.has-next-steps { display: grid; grid-template-columns: minmax(0, 1.55fr) minmax(0, 1fr); align-items: start; gap: 24px; }
.result-order-summary { text-align: left; background: #fafaf6; border: 1px solid #edede5; border-radius: 12px; padding: 18px 20px; }
.result-product { display: flex; gap: 14px; align-items: center; padding-bottom: 17px; border-bottom: 1px solid #e8e8df; }
.result-product > img, .result-product > .product-image-fallback { flex: none; width: 60px; height: 60px; border-radius: 8px; object-fit: cover; }
.result-product > div:nth-child(2) { flex: 1; min-width: 0; }
.result-product h2 { font-size: 15px; margin: 4px 0 5px; line-height: 1.5; }
.result-product p { margin: 0; font-size: 11px; color: #848576; }
.result-price { font-size: 20px; color: var(--color-accent); white-space: nowrap; }
.result-order-meta { display: grid; grid-template-columns: 1fr 1fr; gap: 15px 20px; margin: 17px 0 0; }
.result-order-meta dt { font-size: 10px; color: #8a8b7b; margin-bottom: 5px; }
.result-order-meta dd { margin: 0; font-size: 12px; overflow-wrap: anywhere; }
.next-steps { text-align: left; padding-top: 3px; }
.next-steps > h2 { font-size: 13px; margin: 0 0 14px; }
.next-steps ol { display: grid; grid-template-columns: 1fr; gap: 14px; list-style: none; margin: 0; padding: 0; }
.next-steps li { display: flex; align-items: flex-start; gap: 8px; }
.next-steps li > span { display: grid; place-items: center; flex: none; width: 20px; height: 20px; border-radius: 6px; background: #f1f1e9; font-size: 11px; }
.next-steps strong { font-size: 12px; font-weight: 600; }
.next-steps p { margin: 5px 0 0; font-size: 10px; color: #868776; line-height: 1.7; }
.order-result > .offline-note { margin: 14px 0; }
.result-actions { display: flex; justify-content: center; gap: 12px; }
.flow-secondary { background: white; border-color: #e1e1d6; }
.continue-browsing { display: inline-block; margin-top: 15px; font-size: 12px; color: #858674; }
@media (max-width: 760px) {
  .order-flow-page { padding: 24px 16px 40px; }
  .flow-steps { gap: 30px; }
  .flow-steps li { font-size: 11px; gap: 6px; }
  .flow-steps li + li::before { width: 14px; right: calc(100% + 8px); }
  .checkout-grid { grid-template-columns: 1fr; }
  .checkout-total { position: static; }
  .order-result { padding: 28px 20px; }
  .order-result > h1 { font-size: 23px; }
  .result-order-meta { grid-template-columns: 1fr; }
  .result-body.has-next-steps { grid-template-columns: 1fr; }
  .next-steps ol { grid-template-columns: 1fr; }
  .result-actions { flex-wrap: wrap; }
  .result-actions > * { flex: 1; white-space: nowrap; }
}
</style>
