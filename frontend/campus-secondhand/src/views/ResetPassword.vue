<template>
  <div class="reset-page">
    <div class="animated-bg">
      <div class="gradient-sphere sphere-1"></div>
      <div class="gradient-sphere sphere-2"></div>
      <div class="gradient-sphere sphere-3"></div>
    </div>
    
    <div class="floating-elements">
      <div class="floating-card card-1">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
          <polyline points="22,6 12,13 2,6"/>
        </svg>
        <span>邮件</span>
      </div>
      <div class="floating-card card-2">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
          <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
        </svg>
        <span>安全</span>
      </div>
      <div class="floating-card card-3">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <circle cx="12" cy="12" r="10"/>
          <polyline points="12 6 12 12 16 14"/>
        </svg>
        <span>快速</span>
      </div>
    </div>

    <div class="reset-container">
      <div class="reset-card">
        <div class="card-header">
          <div class="logo-container">
            <svg class="logo-icon" viewBox="0 0 60 60">
              <defs>
                <linearGradient id="resetLogoGrad" x1="0%" y1="0%" x2="100%" y2="100%">
                  <stop offset="0%" style="stop-color:#f59e0b"/>
                  <stop offset="100%" style="stop-color:#ef4444"/>
                </linearGradient>
              </defs>
              <circle cx="30" cy="30" r="28" fill="url(#resetLogoGrad)"/>
              <path d="M20 30 L27 35 L40 24" stroke="white" stroke-width="4" fill="none" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
          <h1 class="brand-title">忘记密码</h1>
          <p class="brand-subtitle">没有邮件重置，登录后在个人中心改密码</p>
        </div>

        <el-form class="reset-form">
          <el-form-item>
            <button 
              type="button" 
              class="submit-btn" 
              @click="onReset"
              :disabled="loading"
            >
              <span class="btn-content">
                <svg v-if="!loading" class="btn-icon" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="22" y1="2" x2="11" y2="13"/>
                  <polygon points="22 2 15 22 11 13 2 9 22 2"/>
                </svg>
                <svg v-else class="spinner" viewBox="0 0 24 24">
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="3" fill="none" stroke-dasharray="31.4 31.4" stroke-linecap="round"/>
                </svg>
                返回登录
              </span>
              <div class="btn-glow"></div>
            </button>
          </el-form-item>
        </el-form>

        <div class="tips-section">
          <div class="tip-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <span>未开通邮件重置，请登录后在个人中心修改密码</span>
          </div>
          <div class="tip-item">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="10"/>
              <line x1="12" y1="16" x2="12" y2="12"/>
              <line x1="12" y1="8" x2="12.01" y2="8"/>
            </svg>
            <span>系统不会发送重置链接</span>
          </div>
        </div>

        <el-divider class="custom-divider">
          <span class="divider-text">或者</span>
        </el-divider>

        <div class="alternative-actions">
          <router-link to="/login" class="alternative-link">
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="19" y1="12" x2="5" y2="12"/>
              <polyline points="12 19 5 12 12 5"/>
            </svg>
            <span>返回登录</span>
          </router-link>
          <span class="separator">|</span>
          <router-link to="/register" class="alternative-link">
            <span>立即注册</span>
            <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
              <circle cx="8.5" cy="7" r="4"/>
              <line x1="20" y1="8" x2="20" y2="14"/>
              <line x1="23" y1="11" x2="17" y2="11"/>
            </svg>
          </router-link>
        </div>
      </div>
      
      <div class="features-banner">
        <div class="banner-title">安全可靠</div>
        <div class="banner-subtitle">我们重视您的账户安全</div>
        <div class="feature-list">
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
              </svg>
            </div>
            <div class="feature-text">
              <div class="feature-name">SSL加密</div>
              <div class="feature-desc">全程加密传输</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
              </svg>
            </div>
            <div class="feature-text">
              <div class="feature-name">安全存储</div>
              <div class="feature-desc">密码加密存储</div>
            </div>
          </div>
          <div class="feature-item">
            <div class="feature-icon">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/>
                <polyline points="22,6 12,13 2,6"/>
              </svg>
            </div>
            <div class="feature-text">
              <div class="feature-name">当面交易</div>
              <div class="feature-desc">不发送重置邮件</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
const router = useRouter()
const loading = ref(false)

function onReset() {
  ElMessage.warning('未开通邮件重置，请登录后在个人中心修改密码')
  router.push({ path: '/login' })
}
</script>

<style scoped>
.reset-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--color-brand);
  position: relative;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, 'Helvetica Neue', Arial, sans-serif;
}

.animated-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}

.gradient-sphere {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.5;
  animation: float 20s ease-in-out infinite;
}

.sphere-1 {
  width: 600px;
  height: 600px;
  background: var(--color-brand);
  top: -200px;
  right: -200px;
  animation-delay: 0s;
}

.sphere-2 {
  width: 400px;
  height: 400px;
  background: var(--color-brand);
  bottom: -100px;
  left: -100px;
  animation-delay: -5s;
}

.sphere-3 {
  width: 300px;
  height: 300px;
  background: var(--color-brand);
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  animation-delay: -10s;
}

@keyframes float {
  0%, 100% { transform: translate(0, 0) scale(1); }
  25% { transform: translate(30px, -30px) scale(1.05); }
  50% { transform: translate(-20px, 20px) scale(0.95); }
  75% { transform: translate(20px, 30px) scale(1.02); }
}

.floating-elements {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.floating-card {
  position: absolute;
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  background: rgba(255, 255, 255, 0.1);
  backdrop-filter: blur(10px);
  border-radius: 16px;
  border: 1px solid rgba(255, 255, 255, 0.2);
  color: var(--color-foreground);
  font-size: 14px;
  font-weight: 500;
  animation: floatCard 15s ease-in-out infinite;
}

.floating-card svg {
  width: 20px;
  height: 20px;
  stroke: rgba(255, 255, 255, 0.8);
}

.card-1 {
  top: 15%;
  right: 10%;
  animation-delay: 0s;
}

.card-2 {
  top: 60%;
  right: 8%;
  animation-delay: -5s;
}

.card-3 {
  bottom: 20%;
  left: 10%;
  animation-delay: -10s;
}

@keyframes floatCard {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-15px) rotate(2deg); }
  50% { transform: translateY(0) rotate(0deg); }
  75% { transform: translateY(15px) rotate(-2deg); }
}

.reset-container {
  display: flex;
  gap: 60px;
  z-index: 10;
  padding: 20px;
}

.reset-card {
  width: 420px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 48px 40px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.card-header {
  text-align: center;
  margin-bottom: 36px;
}

.logo-container {
  margin-bottom: 20px;
}

.logo-icon {
  width: 70px;
  height: 70px;
  filter: drop-shadow(0 10px 20px rgba(245, 158, 11, 0.3));
  animation: pulse 3s ease-in-out infinite;
}

@keyframes pulse {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.05); }
}

.brand-title {
  font-size: 28px;
  font-weight: 700;
  color: var(--color-foreground);
  margin: 0 0 8px 0;
  letter-spacing: 2px;
}

.brand-subtitle {
  font-size: 14px;
  color: var(--color-muted-foreground);
  margin: 0;
}

.reset-form {
  margin-bottom: 24px;
}

.input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.input-icon {
  position: absolute;
  left: 16px;
  width: 20px;
  height: 20px;
  stroke: rgba(255, 255, 255, 0.5);
  z-index: 2;
  transition: stroke 0.3s ease;
}

.input-wrapper:focus-within .input-icon {
  stroke: #f59e0b;
}

.modern-input {
  --el-input-bg-color: rgba(255, 255, 255, 0.1);
  --el-input-border-color: rgba(255, 255, 255, 0.2);
  --el-input-text-color: var(--color-foreground);
  --el-input-placeholder-color: var(--color-muted-foreground);
  --el-input-hover-border-color: rgba(255, 255, 255, 0.3);
  --el-input-focus-border-color: #f59e0b;
  border-radius: 12px;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper) {
  padding-left: 48px;
  background: rgba(255, 255, 255, 0.1);
  border: 1px solid rgba(255, 255, 255, 0.2);
  border-radius: 12px;
  box-shadow: none;
  transition: all 0.3s ease;
}

:deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.3);
  background: rgba(255, 255, 255, 0.12);
}

:deep(.el-input__wrapper.is-focus) {
  background: rgba(255, 255, 255, 0.15);
  border-color: #f59e0b;
  box-shadow: 0 0 0 3px rgba(245, 158, 11, 0.2);
}

:deep(.el-input__inner) {
  color: var(--color-foreground);
  height: 52px;
  font-size: 15px;
}

:deep(.el-input__inner::placeholder) {
  color: var(--color-muted-foreground);
}

.input-highlight {
  position: absolute;
  bottom: 0;
  left: 16px;
  right: 16px;
  height: 2px;
  background: var(--color-brand);
  border-radius: 1px;
}

.submit-btn {
  width: 100%;
  height: 52px;
  border: none;
  border-radius: 12px;
  background: var(--color-brand);
  color: var(--color-foreground);
  font-size: 16px;
  font-weight: 600;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
  margin-top: 8px;
}

.submit-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 10px 30px -10px rgba(245, 158, 11, 0.5);
}

.submit-btn:active:not(:disabled) {
  transform: translateY(0);
}

.submit-btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.btn-content {
  position: relative;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.btn-icon {
  width: 18px;
  height: 18px;
}

.spinner {
  width: 20px;
  height: 20px;
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.btn-glow {
  position: absolute;
  inset: 0;
  background: var(--color-brand);
  opacity: 0;
  transition: opacity 0.3s ease;
}

.submit-btn:hover .btn-glow {
  opacity: 1;
}

.tips-section {
  margin-bottom: 24px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
}

.tip-item {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--color-muted-foreground);
  font-size: 13px;
  margin-bottom: 8px;
}

.tip-item:last-child {
  margin-bottom: 0;
}

.tip-item svg {
  width: 16px;
  height: 16px;
  stroke: rgba(255, 255, 255, 0.4);
  flex-shrink: 0;
}

.custom-divider {
  margin: 24px 0;
  border-color: rgba(255, 255, 255, 0.15);
}

.divider-text {
  color: var(--color-muted-foreground);
  font-size: 13px;
  padding: 0 16px;
  background: transparent;
}

.alternative-actions {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
}

.alternative-link {
  display: flex;
  align-items: center;
  gap: 6px;
  color: var(--color-muted-foreground);
  text-decoration: none;
  font-size: 14px;
  transition: all 0.3s ease;
}

.alternative-link:hover {
  color: #f59e0b;
}

.alternative-link svg {
  width: 16px;
  height: 16px;
}

.separator {
  color: rgba(255, 255, 255, 0.3);
}

.features-banner {
  width: 320px;
  background: rgba(255, 255, 255, 0.08);
  backdrop-filter: blur(20px);
  border-radius: 24px;
  padding: 36px;
  border: 1px solid rgba(255, 255, 255, 0.15);
  align-self: center;
}

.banner-title {
  font-size: 24px;
  font-weight: 700;
  color: var(--color-foreground);
  margin-bottom: 8px;
  text-align: center;
}

.banner-subtitle {
  font-size: 14px;
  color: var(--color-muted-foreground);
  text-align: center;
  margin-bottom: 32px;
}

.feature-list {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.feature-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 16px;
  background: rgba(255, 255, 255, 0.05);
  border-radius: 12px;
  transition: all 0.3s ease;
}

.feature-item:hover {
  background: rgba(255, 255, 255, 0.1);
  transform: translateX(5px);
}

.feature-icon {
  width: 44px;
  height: 44px;
  background: var(--color-brand);
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}

.feature-icon svg {
  width: 22px;
  height: 22px;
  stroke: #f59e0b;
}

.feature-text {
  flex: 1;
}

.feature-name {
  font-size: 15px;
  font-weight: 600;
  color: var(--color-foreground);
  margin-bottom: 4px;
}

.feature-desc {
  font-size: 12px;
  color: var(--color-muted-foreground);
}

@media (max-width: 900px) {
  .reset-container {
    flex-direction: column;
    align-items: center;
  }
  
  .features-banner {
    width: 420px;
    display: none;
  }
}

@media (max-width: 480px) {
  .reset-card {
    width: 100%;
    max-width: 400px;
    padding: 36px 24px;
  }
  
  .floating-card {
    display: none;
  }
}
</style>
