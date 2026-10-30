<script setup>
import { Search, Warning, Loading } from '@element-plus/icons-vue'
defineProps({ loading: Boolean, error: String, empty: Boolean, emptyText: { type: String, default: '没有找到符合条件的记录' } })
defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="admin-list-state" role="status"><Loading class="admin-loading-icon" aria-hidden="true" /><h3>正在加载</h3><p>请稍候，正在获取最新数据。</p></div>
  <div v-else-if="error" class="admin-list-state error-container" role="alert"><Warning aria-hidden="true" /><h3>数据加载失败</h3><p>{{ error }}</p><button class="admin-btn retry-button" type="button" @click="$emit('retry')">重新加载</button></div>
  <div v-else-if="empty" class="admin-list-state" role="status"><Search aria-hidden="true" /><h3>{{ emptyText }}</h3><p>可以调整搜索或筛选条件后再试。</p></div>
  <slot v-else />
</template>
