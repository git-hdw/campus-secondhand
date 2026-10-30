<script setup>
import { ref } from 'vue'
import { Picture, Location } from '@element-plus/icons-vue'
import { formatCatalogAmount as formatPrice } from '../utils/catalogFormat'

defineProps({ items: { type: Array, default: () => [] } })
const failedImages = ref(new Set())
function imageKey(item) { return item.id + ':' + item.img }
function hasImage(item) { return item.img && !item.img.includes('/sample/phone.svg') && !failedImages.value.has(imageKey(item)) }

</script>

<template>
  <div v-if="items.length" class="market-product-grid catalog-grid">
    <RouterLink v-for="item in items" :key="item.id" class="market-item catalog-card" :to="'/products/' + item.id">
      <div class="market-item-image catalog-picture">
        <img v-if="hasImage(item)" :src="item.img" :alt="item.title" loading="lazy" @error="failedImages.add(imageKey(item))" />
        <div v-else class="market-image-placeholder"><Picture aria-hidden="true" /><span>暂无图片</span></div>
        <span v-if="item.pending" class="market-pending catalog-status status-pending">待见面</span>
      </div>
      <div class="market-item-info catalog-body">
        <h3 class="catalog-title" :title="item.title">{{ item.title }}</h3>
        <p class="market-item-price catalog-price"><span class="currency">¥</span>{{ formatPrice(item.price) }}</p>
        <p class="market-item-place catalog-location"><Location aria-hidden="true" />{{ item.place || '见面地点待商议' }}</p>
      </div>
    </RouterLink>
  </div>
  <div v-else class="market-empty">
    <Picture aria-hidden="true" /><h3>这里还没有在售商品</h3><p>把用不上的好物分享给同学吧</p>
    <RouterLink to="/post">发布第一件闲置</RouterLink>
  </div>
</template>

<style scoped>
.market-image-placeholder { position: absolute; inset: 0; display: flex; align-items: center; justify-content: center; flex-direction: column; gap: 10px; color: #999; font-size: 12px; }
.market-image-placeholder svg { width: 36px; height: 36px; color: #aaa; }
.market-empty { text-align: center; padding: 48px 20px; background: white; border: 1px solid #e8e8e8; border-radius: 12px; }
.market-empty > svg { width: 40px; height: 40px; color: #aaa; }
.market-empty h3 { font-size: 16px; margin: 16px 0 8px; }
.market-empty p { font-size: 13px; color: #777; margin: 0 0 20px; }
.market-empty a { display: inline-block; padding: 10px 18px; background: var(--color-brand); border-radius: 8px; text-decoration: none; color: #1a1a1a; font-size: 13px; }
</style>
