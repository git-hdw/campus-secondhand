<template>
  <div class="register-page auth-page">
    <div class="register-container auth-container">
      <div class="register-card auth-card">
        <div class="card-header">
          <div class="logo-container">
            <svg class="logo-icon" viewBox="0 0 60 60">
              <circle cx="30" cy="30" r="28" fill="currentColor"/>
              <path d="M20 30 L27 35 L40 24" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1 class="brand-title">加入我们</h1>
          <p class="brand-subtitle">创建您的校园二手交易系统账号</p>
        </div>

        <el-form :model="form" :rules="rules" ref="regForm" class="register-form auth-form" @submit.prevent="onRegister">
          <el-form-item prop="username" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                <circle cx="12" cy="7" r="4"/>
              </svg>
              <input 
                v-model="form.username" aria-label="用户名" autocomplete="username" name="username"
                placeholder="请输入用户名" 
                class="modern-input"
              />
            </div>
          </el-form-item>

          <el-form-item prop="studentNo" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" aria-hidden="true"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M7 10h4M7 14h10M15 10h2"/></svg>
              <input
                v-model="form.studentNo" aria-label="学号" autocomplete="off" name="studentNo"
                placeholder="请输入学号，6到20位字母或数字"
                class="modern-input"
                maxlength="20"
              />
            </div>
          </el-form-item>

          <el-form-item prop="email" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
              <input 
                v-model="form.email" aria-label="邮箱" autocomplete="email" name="email"
                type="email"
                placeholder="请输入邮箱地址" 
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
                v-model="form.password" aria-label="密码" autocomplete="new-password" name="password"
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

          <el-form-item prop="confirm" class="form-field">
            <div class="input-wrapper">
              <svg class="input-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"/>
                <polyline points="22 4 12 14.01 9 11.01"/>
              </svg>
              <input 
                v-model="form.confirm" aria-label="确认密码" autocomplete="new-password" name="confirm"
                :type="showConfirmPassword ? 'text' : 'password'" 
                placeholder="请确认密码" 
                class="modern-input"
              />
              <button type="button" class="password-toggle" aria-label="显示或隐藏确认密码" @click="showConfirmPassword = !showConfirmPassword">
                <svg v-if="!showConfirmPassword" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
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

          <div class="password-strength" v-if="form.password">
            <div class="strength-label">密码强度</div>
            <div class="strength-bar">
              <div class="strength-fill" :class="strengthClass" :style="{ width: strengthWidth }"></div>
            </div>
            <div class="strength-text" :class="strengthClass">{{ strengthText }}</div>
          </div>

          <el-form-item class="submit-item">
            <button type="submit" class="submit-btn" :disabled="loading">
              <span class="btn-text">{{ loading ? '注册中…' : '创建账号' }}</span>
              <svg class="btn-arrow" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="5" y1="12" x2="19" y2="12"/>
                <polyline points="12 5 19 12 12 19"/>
              </svg>
            </button>
          </el-form-item>
        </el-form>

        <div class="divider">
          <span>已有账号？</span>
        </div>

        <router-link :to="loginLocation()" class="login-link auth-switch-link">
          <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4"/>
            <polyline points="10 17 15 12 10 7"/>
            <line x1="15" y1="12" x2="3" y2="12"/>
          </svg>
          <span>立即登录</span>
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
defineOptions({ name: 'RegisterPage' })
import { reactive, ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import auth from '../api/auth'

const router = useRouter()
const route = useRoute()

function loginLocation() {
  return { path: '/login', query: route.query.redirect ? { redirect: route.query.redirect } : {} }
}
const regForm = ref(null)
const loading = ref(false)
const showPassword = ref(false)
const showConfirmPassword = ref(false)

const form = reactive({ 
  username: '', 
  studentNo: '',
  email: '', 
  password: '', 
  confirm: '' 
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度3-20字符', trigger: 'blur' }
  ],
  studentNo: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^[A-Za-z0-9]{6,20}$/, message: '学号需要 6 到 20 位字母或数字', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度6-20字符', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请确认密码', trigger: 'blur' }
  ],
}

const strengthScore = computed(() => {
  const password = form.password
  if (!password) return 0
  
  let score = 0
  if (password.length >= 6) score += 1
  if (password.length >= 10) score += 1
  if (/[A-Z]/.test(password)) score += 1
  if (/[a-z]/.test(password)) score += 1
  if (/[0-9]/.test(password)) score += 1
  if (/[^A-Za-z0-9]/.test(password)) score += 1
  
  return Math.min(score, 5)
})

const strengthClass = computed(() => {
  if (strengthScore.value === 0) return 'none'
  if (strengthScore.value <= 2) return 'weak'
  if (strengthScore.value <= 3) return 'medium'
  return 'strong'
})

const strengthWidth = computed(() => `${(strengthScore.value / 5) * 100}%`)

const strengthText = computed(() => {
  switch (strengthClass.value) {
    case 'weak': return '弱'
    case 'medium': return '中'
    case 'strong': return '强'
    default: return ''
  }
})

function onRegister() {
  if (loading.value) return
  regForm.value.validate((valid) => {
    if (valid) {
      if (form.password !== form.confirm) {
        ElMessage.error('两次输入的密码不一致')
        return
      }
      
      loading.value = true
      auth
        .register({ 
          username: form.username,
          studentNo: form.studentNo,
          email: form.email, 
          password: form.password, 
          confirmPassword: form.confirm 
        })
        .then(() => {
          ElMessage.success({
            message: '注册成功！欢迎加入校园二手交易系统',
            type: 'success',
            duration: 2000
          })
          setTimeout(() => {
            router.push(loginLocation())
          }, 1500)
        })
        .catch((err) => {
          ElMessage.error(err.message || '注册失败，请重试')
        })
        .finally(() => {
          loading.value = false
        })
    }
  })
}
</script>

<style src="../styles/auth.css"></style>
<style scoped>

.password-strength {
  margin-bottom: 20px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 8px;
}

.strength-label {
  font-size: 12px;
  color: var(--color-muted-foreground);
  margin-bottom: 8px;
}

.strength-bar {
  width: 100%;
  height: 4px;
  background: rgba(255, 255, 255, 0.2);
  border-radius: 2px;
  overflow: hidden;
  margin-bottom: 6px;
}

.strength-fill {
  height: 100%;
  transition: all 0.3s ease;
  border-radius: 2px;
}

.strength-fill.weak {
  background: var(--color-brand);
}

.strength-fill.medium {
  background: var(--color-brand);
}

.strength-fill.strong {
  background: var(--color-brand);
}

.strength-text {
  font-size: 11px;
  font-weight: 500;
}

.strength-text.weak {
  color: #ef4444;
}

.strength-text.medium {
  color: #f59e0b;
}

.strength-text.strong {
  color: var(--color-foreground);
}

</style>
