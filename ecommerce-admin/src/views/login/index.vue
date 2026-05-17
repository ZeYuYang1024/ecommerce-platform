<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="bg-gradient"></div>
      <div class="bg-dots"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <div class="logo-circle">
          <svg width="32" height="32" viewBox="0 0 28 28" fill="none">
            <defs>
              <linearGradient id="login-logo-grad" x1="0" y1="0" x2="28" y2="28">
                <stop stop-color="#C8963E"/>
                <stop offset="1" stop-color="#E8C876"/>
              </linearGradient>
            </defs>
            <rect width="28" height="28" rx="8" fill="url(#login-logo-grad)"/>
            <path d="M8 10l6-4 6 4v8l-6 4-6-4V10z" stroke="#1A1816" stroke-width="1.8" fill="none"/>
            <circle cx="14" cy="14" r="2.5" fill="#1A1816"/>
          </svg>
        </div>
        <h1>MERCH<span class="accent">PANEL</span></h1>
        <p>登录管理后台</p>
      </div>
      <el-form :model="form" :rules="rules" ref="formRef" class="login-form">
        <div class="field-group">
          <label>用户名</label>
          <el-input v-model="form.username" placeholder="请输入用户名" size="large" />
        </div>
        <div class="field-group">
          <label>密码</label>
          <el-input v-model="form.password" :type="pwdType" placeholder="请输入密码" size="large">
            <template #suffix>
              <span class="pwd-toggle" @click="togglePwd">
                <el-icon v-if="pwdType === 'password'"><View /></el-icon>
                <el-icon v-else><Hide /></el-icon>
              </span>
            </template>
          </el-input>
        </div>
        <el-button type="primary" size="large" class="submit-btn" @click="login" :loading="loading">
          登 录
        </el-button>
      </el-form>
      <div class="divider"><span>或</span></div>
      <router-link to="/merchant/register" class="merchant-entry">
        <div class="merchant-entry-icon">
          <el-icon :size="22"><Shop /></el-icon>
        </div>
        <div class="merchant-entry-text">
          <span class="merchant-entry-title">商家入驻</span>
          <span class="merchant-entry-desc">开通店铺，开始销售</span>
        </div>
        <el-icon :size="16" class="merchant-entry-arrow"><ArrowRight /></el-icon>
      </router-link>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { View, Hide } from '@element-plus/icons-vue'
import axios from 'axios'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const pwdType = ref('password')
const form = reactive({ username: '', password: '' })

function togglePwd() {
  pwdType.value = pwdType.value === 'password' ? 'text' : 'password'
}
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function login() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  loading.value = true
  try {
    const { data } = await axios.post('/api/v1/auth/admin/login', form)
    if (data.code === 200) {
      localStorage.setItem('token', data.data.token)
      localStorage.setItem('username', data.data.username)
      localStorage.setItem('type', data.data.type || 'super_admin')
      if (data.data.merchantId !== null && data.data.merchantId !== undefined) {
        localStorage.setItem('merchantId', String(data.data.merchantId))
      } else {
        localStorage.removeItem('merchantId')
      }
      router.push('/dashboard')
    } else {
      ElMessage.error(data.message)
    }
  } catch {
    ElMessage.error('登录失败')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  overflow: hidden;
  background: #1A1816;
}
.login-bg {
  position: absolute;
  inset: 0;
  overflow: hidden;
}
.bg-gradient {
  position: absolute;
  inset: 0;
  background:
    radial-gradient(ellipse at 25% 30%, rgba(200, 150, 62, 0.12) 0%, transparent 50%),
    radial-gradient(ellipse at 75% 70%, rgba(200, 150, 62, 0.06) 0%, transparent 50%),
    radial-gradient(ellipse at 50% 50%, rgba(232, 213, 176, 0.04) 0%, transparent 60%);
}
.bg-dots {
  position: absolute;
  inset: 0;
  background-image:
    radial-gradient(circle, rgba(255,255,255,0.04) 1px, transparent 1px);
  background-size: 48px 48px;
}

.login-card {
  position: relative;
  width: 440px;
  background: rgba(30, 41, 59, 0.95);
  backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.08);
  border-radius: var(--radius-xl);
  padding: 44px 40px;
  box-shadow: 0 25px 50px -12px rgba(0, 0, 0, 0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 36px;
}
.logo-circle {
  width: 56px;
  height: 56px;
  border-radius: var(--radius-xl);
  background: rgba(200, 150, 62, 0.15);
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
}
.login-header h1 {
  font-size: 24px;
  font-weight: 800;
  letter-spacing: 0.04em;
  color: #ffffff;
  margin-bottom: 6px;
}
.login-header .accent { color: var(--accent); }
.login-header p {
  font-size: 13.5px;
  color: rgba(148, 163, 184, 0.8);
}

.field-group {
  margin-bottom: 20px;
}
.field-group label {
  display: block;
  font-size: 12.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: rgba(148, 163, 184, 0.8);
  margin-bottom: 8px;
}

.field-group :deep(.el-input__wrapper) {
  background: rgba(255, 255, 255, 0.05) !important;
  border: 1px solid rgba(255, 255, 255, 0.1) !important;
  border-radius: var(--radius) !important;
  box-shadow: none !important;
}
.field-group :deep(.el-input__wrapper:hover) {
  border-color: rgba(255, 255, 255, 0.2) !important;
}
.field-group :deep(.el-input__wrapper.is-focus) {
  border-color: var(--accent) !important;
  box-shadow: 0 0 0 3px rgba(200, 150, 62, 0.15) !important;
}
.field-group :deep(.el-input__inner) {
  color: #ffffff !important;
}
.field-group :deep(.el-input__inner::placeholder) {
  color: rgba(148, 163, 184, 0.5) !important;
}

.submit-btn {
  width: 100%;
  height: 46px !important;
  font-size: 15px !important;
  font-weight: 700 !important;
  letter-spacing: 0.04em !important;
  margin-top: 12px;
  border-radius: var(--radius) !important;
  background: var(--accent) !important;
  border-color: var(--accent) !important;
}
.submit-btn:hover {
  background: var(--accent-hover) !important;
  border-color: var(--accent-hover) !important;
  box-shadow: 0 8px 20px rgba(200, 150, 62, 0.35) !important;
  transform: translateY(-1px);
}

.pwd-toggle {
  display: inline-flex;
  align-items: center;
  cursor: pointer;
  color: rgba(148, 163, 184, 0.6);
  padding: 0 4px;
  user-select: none;
  transition: color var(--transition-fast);
}
.pwd-toggle:hover {
  color: rgba(226, 232, 240, 0.8);
}

/* Divider */
.divider {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 20px 0 16px;
}
.divider::before,
.divider::after {
  content: '';
  flex: 1;
  height: 1px;
  background: rgba(148, 163, 184, 0.15);
}
.divider span {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.4);
  text-transform: uppercase;
  letter-spacing: 0.06em;
}

/* Merchant entry card */
.merchant-entry {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 16px 20px;
  background: rgba(200, 150, 62, 0.06);
  border: 1px solid rgba(200, 150, 62, 0.15);
  border-radius: 14px;
  text-decoration: none;
  transition: all 0.2s ease;
  cursor: pointer;
}
.merchant-entry:hover {
  background: rgba(200, 150, 62, 0.12);
  border-color: rgba(200, 150, 62, 0.3);
  transform: translateY(-1px);
}
.merchant-entry-icon {
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(200, 150, 62, 0.2), rgba(232, 200, 118, 0.2));
  display: flex;
  align-items: center;
  justify-content: center;
  color: #C8963E;
  flex-shrink: 0;
}
.merchant-entry-text {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 2px;
}
.merchant-entry-title {
  font-size: 15px;
  font-weight: 600;
  color: rgba(226, 232, 240, 0.9);
}
.merchant-entry-desc {
  font-size: 12px;
  color: rgba(148, 163, 184, 0.5);
}
.merchant-entry-arrow {
  color: rgba(148, 163, 184, 0.3);
  flex-shrink: 0;
  transition: transform 0.2s;
}
.merchant-entry:hover .merchant-entry-arrow {
  transform: translateX(3px);
  color: #C8963E;
}
</style>
