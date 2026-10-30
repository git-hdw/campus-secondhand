<template>
  <div class="recom-list">
    <div 
      class="recom" 
      v-for="(r, i) in items" 
      :key="i"
      @click="$emit('click', r)"
    >
      <div class="dot"></div>
      <div class="body">
        <div class="title">{{ r.title }}</div>
        <div class="desc">{{ r.desc }}</div>
      </div>
      <div class="arrow">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <line x1="5" y1="12" x2="19" y2="12"/>
          <polyline points="12 5 19 12 12 19"/>
        </svg>
      </div>
    </div>
    <div v-if="!items || items.length===0" class="empty">暂无推荐</div>
  </div>
</template>

<script setup>
import { defineProps, defineEmits } from 'vue'
const props = defineProps({ items: { type: Array, default: () => [] } })
const emit = defineEmits(['click'])
</script>

<style scoped>
.recom-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.recom {
  display: flex;
  gap: 12px;
  align-items: center;
  padding: 12px 16px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.03);
  border: 1px solid rgba(255, 255, 255, 0.08);
  cursor: pointer;
  transition: all 0.3s ease;
}

.recom:hover {
  background: rgba(255, 255, 255, 0.08);
  border-color: rgba(16, 185, 129, 0.3);
  transform: translateX(4px);
}

.dot {
  width: 10px;
  height: 10px;
  background: linear-gradient(135deg, #10b981, #3b82f6);
  border-radius: 50%;
  flex-shrink: 0;
  box-shadow: 0 0 0 3px rgba(16, 185, 129, 0.2);
}

.body {
  flex: 1;
  min-width: 0;
}

.title {
  font-weight: 600;
  font-size: 15px;
  color: var(--color-foreground);
  margin-bottom: 4px;
}

.desc {
  font-size: 13px;
  color: var(--color-muted-foreground);
}

.arrow {
  width: 28px;
  height: 28px;
  border-radius: 8px;
  background: linear-gradient(135deg, rgba(16, 185, 129, 0.15), rgba(59, 130, 246, 0.15));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #10b981;
  flex-shrink: 0;
  transition: all 0.3s ease;
  opacity: 0;
  transform: translateX(-8px);
}

.recom:hover .arrow {
  opacity: 1;
  transform: translateX(0);
}

.arrow svg {
  width: 16px;
  height: 16px;
}

.empty {
  color: var(--color-muted-foreground);
  padding: 24px;
  text-align: center;
  font-size: 14px;
}
</style>
