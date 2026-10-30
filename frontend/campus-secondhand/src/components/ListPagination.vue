<script setup>
defineProps({ page: { type: Number, required: true }, total: { type: Number, required: true }, pageSize: { type: Number, default: 20 }, busy: Boolean })
defineEmits(['change'])
</script>
<template>
  <nav v-if="total > pageSize" class="list-pagination" aria-label="列表分页">
    <button type="button" :disabled="busy || page <= 1" @click="$emit('change', page - 1)">上一页</button>
    <span>第 {{ page }} / {{ Math.max(1, Math.ceil(total / pageSize)) }} 页 · 共 {{ total }} 条</span>
    <button type="button" :disabled="busy || page * pageSize >= total" @click="$emit('change', page + 1)">下一页</button>
  </nav>
</template>
<style scoped>
.list-pagination { display: flex; align-items: center; justify-content: center; flex-wrap: wrap; gap: 12px; padding: 16px 0; font-size: 13px; }
button { border: 1px solid var(--color-border, #ddd); background: white; border-radius: 8px; padding: 8px 12px; cursor: pointer; }
button:disabled { opacity: .45; cursor: default; }
</style>
