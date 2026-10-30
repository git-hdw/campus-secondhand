<template>
  <el-card class="dash-card" :body-style="{padding:'16px'}" @click="handleClick" :class="{ clickable: clickable }">
    <div class="top">
      <div class="icon" :style="{background: color}">
        <el-icon><component :is="iconName" /></el-icon>
      </div>
      <div class="meta">
        <div class="value">{{ value }}</div>
        <div class="title">{{ title }}</div>
      </div>
    </div>
  </el-card>
</template>

<script setup>
import { computed } from 'vue'
import { ShoppingCart, List, ChatLineRound, User } from '@element-plus/icons-vue'

const props = defineProps({ title: String, value: [String, Number], icon: String, color: String, clickable: { type: Boolean, default: false } })
const emit = defineEmits(['click'])

const iconName = computed(() => {
  const icons = { shopping: ShoppingCart, order: List, message: ChatLineRound, user: User }
  return icons[props.icon] || ShoppingCart
})

function handleClick() {
  if (props.clickable) {
    emit('click')
  }
}
</script>

<style scoped>
.dash-card{border-radius:10px; transition: all 0.3s ease;}
.dash-card.clickable{cursor: pointer;}
.dash-card.clickable:hover{transform: translateY(-4px); box-shadow: 0 12px 24px rgba(0,0,0,0.15);}
.top{display:flex;align-items:center;gap:12px}
.icon{width:56px;height:56px;border-radius:10px;display:flex;align-items:center;justify-content:center;color:white}
.meta .value{font-size:20px;font-weight:600}
.meta .title{color:#888;margin-top:6px}
</style>
