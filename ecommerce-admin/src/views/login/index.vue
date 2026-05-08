<template>
  <div class="login-page">
    <div class="login-bg">
      <div class="grid-overlay"></div>
    </div>
    <div class="login-card">
      <div class="login-header">
        <svg width="36" height="36" viewBox="0 0 28 28" fill="none">
          <rect width="28" height="28" rx="6" fill="#e6a820"/>
          <path d="M8 10l6-4 6 4v8l-6 4-6-4V10z" stroke="#000" stroke-width="1.5" fill="none"/>
          <circle cx="14" cy="14" r="2.5" fill="#000"/>
        </svg>
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
          <el-input v-model="form.password" type="password" placeholder="请输入密码" size="large" show-password />
        </div>
        <el-button type="primary" size="large" class="submit-btn" @click="login" :loading="loading">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const router = useRouter()
const formRef = ref(null)
const loading = ref(false)
const form = reactive({ username: '', password: '' })
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
}
.login-bg {
  position: absolute;
  inset: 0;
  background: radial-gradient(ellipse at 30% 50%, rgba(230,168,32,0.04) 0%, transparent 60%),
              radial-gradient(ellipse at 70% 20%, rgba(96,165,250,0.03) 0%, transparent 50%);
}
.grid-overlay {
  position: absolute;
  inset: 0;
  background-image:
    linear-gradient(rgba(255,255,255,0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255,255,255,0.02) 1px, transparent 1px);
  background-size: 60px 60px;
}

.login-card {
  position: relative;
  width: 420px;
  background: var(--bg-card);
  border: 1px solid var(--border-default);
  border-radius: var(--radius-lg);
  padding: 40px;
  box-shadow: 0 4px 24px rgba(0,0,0,0.5);
}

.login-header {
  text-align: center;
  margin-bottom: 32px;
}
.login-header svg { margin: 0 auto 16px; display: block; }
.login-header h1 {
  font-size: 22px;
  font-weight: 700;
  letter-spacing: 0.05em;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.login-header .accent { color: var(--accent); }
.login-header p {
  font-size: 13px;
  color: var(--text-muted);
}

.field-group {
  margin-bottom: 18px;
}
.field-group label {
  display: block;
  font-size: 12px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.08em;
  color: var(--text-muted);
  margin-bottom: 6px;
}

.submit-btn {
  width: 100%;
  height: 44px !important;
  font-size: 14px !important;
  font-weight: 600 !important;
  letter-spacing: 0.03em !important;
  margin-top: 8px;
}
</style>
