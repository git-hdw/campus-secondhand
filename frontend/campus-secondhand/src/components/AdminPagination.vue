<script setup>
defineProps({ page: Number, pageSize: Number, total: Number, totalPages: Number, loading: Boolean })
defineEmits(['change', 'resize'])
</script>

<template>
  <div v-if="total > 0" class="admin-pagination">
    <span class="pagination-info">共 {{ total }} 条记录 · 第 {{ page }} / {{ totalPages }} 页</span>
    <div class="admin-pagination-controls">
      <label>每页 <select :value="pageSize" aria-label="每页条数" :disabled="loading" @change="$emit('resize', Number($event.target.value))"><option :value="10">10 条</option><option :value="20">20 条</option><option :value="50">50 条</option></select></label>
      <button class="pagination-button" type="button" :disabled="page <= 1 || loading" @click="$emit('change', page - 1)">上一页</button>
      <span class="admin-current-page" aria-current="page">{{ page }}</span>
      <button class="pagination-button" type="button" :disabled="page >= totalPages || loading" @click="$emit('change', page + 1)">下一页</button>
    </div>
  </div>
</template>
