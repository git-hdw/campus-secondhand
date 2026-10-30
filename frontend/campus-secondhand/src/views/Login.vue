<template>
  <div class="login-page auth-page">
    <div class="login-container auth-container">
      <div class="login-card auth-card">
        <div class="card-header">
          <div class="logo-container">
            <svg class="logo-icon" viewBox="0 0 60 60">
              <circle cx="30" cy="30" r="28" fill="currentColor"/>
              <path d="M20 30 L27 35 L40 24" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1 class="brand-title">校园二手交易系统</h1>
          <p class="brand-subtitle">当面看货，当面交易</p>
        </div>

        <el-form :model="form" :rules="rules" ref="loginForm" class="login-form auth-form" @submit.prevent="onSubmit">
          <el-form-item prop="username" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input 
                v-model="form.username" aria-label="用户名或学号" autocomplete="username" name="username"
                placeholder="请输入用户名或学号" 
                class="modern-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="password" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
              <input 
                v-model="form.password" aria-label="密码" autocomplete="current-password" name="password"
                :type="showPassword ? 'text' : 'password'" 
                placeholder="请输入密码" 
                class="modern-input"
              />
              <button type="button" class="password-toggle" aria-label="显示或隐藏密码" @click="showPassword = !showPassword">
                <svg v-if="!showPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                  <circle cx="12" cy="12" r="3"/>
                </svg>
                <svg v-else viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24"/>
                  <line x1="1" y1="1" x2="23" y2="23"/>
                </svg>
              </button>
            </div>
          </el-form-item>

          <div class="form-options">
            <label class="remember-me">
              <input type="checkbox" v-model="form.remember" />
              <span class="label-text">记住我</span>
            </label>
            <router-link to="/forgot" class="forgot-link">忘记密码？</router-link>
          </div>

          <el-form-item class="submit-item">
            <button type="submit" class="submit-btn" :disabled="loading">
              <span class="btn-text">{{ loading ? '登录中…' : '登录' }}</span>
              <svg class="btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </button>
          </el-form-item>
        </el-form>

        <div class="divider">
          <span>还没有账号？</span>
        </div>

        <router-link :to="{ path: '/register', query: route.query.redirect ? { redirect: route.query.redirect } : {} }" class="register-link auth-switch-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
            <circle cx="8.5" cy="7" r="4"/>
            <line x1="20" y1="8" x2="20" y2="14"/>
            <line x1="23" y1="11" x2="17" y2="11"/>
          </svg>
          <span>立即注册</span>
        </router-link>
        <RouterLink to="/" class="auth-home">← 返回首页</RouterLink>
      </div>

      <div class="features-banner">
        <div class="feature">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
            <polyline points="22 4 12 14.01 9 11.01"/>
          </svg>
          <span>当面验货</span>
        </div>
        <div class="feature">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <circle cx="12" cy="12" r="10"/>
            <polyline points="12 6 12 12 16 14"/>
          </svg>
          <span>先问再买</span>
        </div>
        <div class="feature">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M20.84 4.61a5.5 5.5 0 0 0-7.78 0L12 5.67l-1.06-1.06a5.5 5.5 0 0 0-7.78 7.78l1.06 1.06L12 21.23l7.78-7.78 1.06-1.06a5.5 5.5 0 0 0 0-7.78z"/>
          </svg>
          <span>见面交易</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
defineOptions({ name: 'LoginPage' })
import { reactive, ref } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'

const router = useRouter()
const route = useRoute()
const loginForm = ref(null)
const loading = ref(false)
const showPassword = ref(false)

const form = reactive({
  username: '',
  password: '',
  remember: false,
})

const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

function onSubmit() {
  if (loading.value) return
  loginForm.value.validate((valid) => {
    if (valid) {
      loading.value = true
      const username = form.username?.trim()
      const password = form.password
      
      auth.login({ username, password })
        .then((res) => {
          const token = res?.token
          const user = res?.user
          if (token && user) {
            auth.clearAuth()
            if (form.remember) {
              localStorage.setItem('token', token)
              localStorage.setItem('user', JSON.stringify(user))
            } else {
              sessionStorage.setItem('token', token)
              sessionStorage.setItem('user', JSON.stringify(user))
            }
            ElMessage.success({
              message: '登录成功，欢迎回来！',
              type: 'success',
              duration: 2000
            })
            const raw = route.query.redirect
            const redirect = typeof raw === 'string' && raw.startsWith('/') && !raw.startsWith('//') ? raw : '/'
            router.push(redirect)
          } else {
            ElMessage.error('登录返回缺少token或用户信息')
          }
        })
        .catch((err) => {
          ElMessage.error(err.message || '登录失败，请检查用户名和密码')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}
</script>

<style src="../styles/auth.css"></style>
