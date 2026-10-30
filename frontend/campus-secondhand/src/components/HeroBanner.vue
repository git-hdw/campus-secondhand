<template>
  <div class="hero">
    <div class="hero-inner container">
      <div class="hero-left">
        <h1>欢迎回到校园二手平台</h1>
        <p class="lead">安全、便捷、环保 —— 在这里买卖闲置，延长物品寿命。</p>

        <div class="hero-actions">
          <el-input v-model="query" placeholder="搜索想要的物品" clearable class="search-input" @keyup.enter="onSearch">
            <template #append>
              <el-button type="primary" @click="onSearch">搜索</el-button>
            </template>
          </el-input>
          <el-button type="primary" plain @click="go('/post')">我要发布</el-button>
        </div>
      </div>

      <div class="hero-right">
        <el-card class="hero-card" shadow="hover">
          <img src="/sample/hero.svg" alt="hero" />
        </el-card>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
const router = useRouter()
const query = ref('')
function onSearch(){ if(query.value) router.push({ path: '/search', query: { q: query.value } }) }
function go(path){ router.push(path) }
</script>

<style scoped>
.hero{padding:56px 0;background:linear-gradient(90deg,#5b8ff9 0%,#7bd389 100%);color:white}
.hero-inner{display:flex;align-items:center;justify-content:space-between;gap:20px}
.hero-left{max-width:620px}
.hero-left h1{font-size:32px;margin:0 0 10px}
.lead{opacity:0.95;margin-bottom:18px}
.hero-actions{display:flex;align-items:center;gap:12px}
.search-input{width:360px}
.hero-right img{max-width:320px}
.hero-card{padding:12px;background:rgba(255,255,255,0.9)}
@media(max-width:900px){.hero-inner{flex-direction:column}.search-input{width:100%}}
</style>
