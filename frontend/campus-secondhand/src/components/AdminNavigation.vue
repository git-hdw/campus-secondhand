<script setup>
import { useRoute } from 'vue-router'
import { Odometer, User, Goods, List, Folder, ChatDotRound, ArrowRight } from '@element-plus/icons-vue'

const route = useRoute()
const items = [
  { path: '/admin', label: '管理概览', icon: Odometer },
  { path: '/admin/users', label: '用户管理', icon: User },
  { path: '/admin/products', label: '商品管理', icon: Goods },
  { path: '/admin/orders', label: '订单管理', icon: List },
  { path: '/admin/categories', label: '分类管理', icon: Folder },
  { path: '/admin/messages', label: '消息管理', icon: ChatDotRound },
]
</script>

<template>
  <nav class="admin-workspace-nav" aria-label="管理导航">
    <div class="workspace-nav-heading">
      <span class="workspace-nav-title">管理工作台</span>
      <RouterLink class="back-to-campus" to="/">
        返回首页 <ArrowRight aria-hidden="true" />
      </RouterLink>
    </div>
    <div class="workspace-nav-grid">
      <RouterLink
        v-for="item in items"
        :key="item.path"
        :to="item.path"
        class="workspace-nav-card"
        :aria-current="route.path === item.path ? 'page' : undefined"
      >
        <span class="workspace-nav-icon" aria-hidden="true"><component :is="item.icon" /></span>
        <span class="workspace-nav-label">{{ item.label }}</span>
        <span v-if="route.path === item.path" class="workspace-nav-indicator" aria-hidden="true"></span>
      </RouterLink>
    </div>
  </nav>
</template>

<style scoped>
.admin-workspace-nav { max-width: 1240px; margin: 0 auto; padding: 26px 28px 0; box-sizing: border-box; }
.workspace-nav-heading { display: flex; align-items: center; justify-content: space-between; gap: 20px; margin-bottom: 14px; }
.workspace-nav-title { color: #77776c; font-size: 13px; font-weight: 500; letter-spacing: 1px; }
.back-to-campus { display: inline-flex; align-items: center; gap: 7px; padding: 4px 0; color: #77776c; font-size: 13px; text-decoration: none; transition: color .18s ease; }
.back-to-campus svg { width: 14px; height: 14px; transition: transform .18s ease; }
.back-to-campus:hover { color: #1a1a1a; }
.back-to-campus:hover svg { transform: translateX(3px); }
.workspace-nav-grid { display: grid; grid-template-columns: repeat(6, minmax(0, 1fr)); gap: 14px; }
.workspace-nav-card { position: relative; display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 12px; min-height: 112px; padding: 16px; box-sizing: border-box; background: white; border: 1px solid #e8e8e1; border-radius: 14px; color: #55554d; text-decoration: none; transition: border-color .18s ease, background .18s ease, color .18s ease, box-shadow .18s ease, transform .18s ease; }
.workspace-nav-icon { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 11px; background: #f6f6f1; transition: background .18s ease; }
.workspace-nav-icon svg { width: 23px; height: 23px; }
.workspace-nav-label { font-size: 14px; font-weight: 500; line-height: 1.4; white-space: nowrap; }
.workspace-nav-card:hover { border-color: #c5c5b9; color: #1a1a1a; box-shadow: 0 6px 16px #25251a0a; transform: translateY(-2px); }
.workspace-nav-card:hover .workspace-nav-icon { background: #fff9c7; }
.workspace-nav-card[aria-current="page"] { background: #242420; border-color: #242420; color: white; box-shadow: 0 5px 14px #25251a12; }
.workspace-nav-card[aria-current="page"] .workspace-nav-icon { background: var(--color-brand); color: #242420; }
.workspace-nav-card[aria-current="page"] .workspace-nav-label { font-weight: 650; }
.workspace-nav-indicator { position: absolute; top: 13px; right: 13px; width: 5px; height: 5px; background: var(--color-brand); border-radius: 50%; }
.workspace-nav-card:focus-visible, .back-to-campus:focus-visible { outline: 2px solid #1a1a1a; outline-offset: 4px; }
@media (max-width: 760px) {
  .admin-workspace-nav { padding: 20px 16px 0; }
  .workspace-nav-grid { grid-template-columns: repeat(3, minmax(0, 1fr)); gap: 10px; }
  .workspace-nav-card { min-height: 100px; padding: 12px; gap: 9px; }
}
@media (prefers-reduced-motion: reduce) {
  .workspace-nav-card, .workspace-nav-icon, .back-to-campus, .back-to-campus svg { transition: none; }
  .workspace-nav-card:hover, .back-to-campus:hover svg { transform: none; }
}
</style>
