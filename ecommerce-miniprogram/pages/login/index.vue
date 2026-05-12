<template>
  <view class="page">
    <view class="card">
      <view class="logo">S</view>
      <text class="title">品质商城</text>
      <text class="subtitle">品质好物 一站购齐</text>
    </view>
    <view class="form">
      <input v-model="username" placeholder="用户名" class="input" />
      <input v-model="password" password placeholder="密码" class="input" />
      <button class="btn" @tap="doLogin" :disabled="loading">{{ loading ? '登录中...' : '登录' }}</button>
      <button class="btn-wx" @tap="doWxLogin">微信一键登录</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'

const username = ref('')
const password = ref('')
const loading = ref(false)

async function doLogin() {
  if (!username.value || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await new Promise((resolve) => {
      uni.request({
        url: 'http://192.168.5.6:8080/api/v1/auth/login',
        method: 'POST',
        data: { username: username.value, password: password.value },
        header: { 'Content-Type': 'application/json' },
        success(r) { resolve(r.data) },
        fail() { resolve({ code: -1, message: '网络错误' }) }
      })
    })
    if (res.code === 200) {
      uni.setStorageSync('token', res.data.token)
      uni.showToast({ title: '登录成功', icon: 'success' })
      setTimeout(() => {
        const pages = getCurrentPages()
        if (pages.length > 1) {
          uni.navigateBack()
        } else {
          uni.switchTab({ url: '/pages/index/index' })
        }
      }, 500)
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

function doWxLogin() {
  uni.showToast({ title: '请使用微信登录', icon: 'none' })
}
</script>

<style>
.page { min-height: 100vh; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60rpx 48rpx; background: #F8F8F8; }
.card { text-align: center; margin-bottom: 60rpx; }
.logo { width: 120rpx; height: 120rpx; background: linear-gradient(135deg, #F59E0B, #F97316); border-radius: 30rpx; display: flex; align-items: center; justify-content: center; font-size: 56rpx; color: #fff; margin: 0 auto 24rpx; }
.title { font-size: 40rpx; font-weight: bold; display: block; color: #1F2937; }
.subtitle { font-size: 26rpx; color: #9CA3AF; display: block; margin-top: 8rpx; }
.form { width: 100%; max-width: 600rpx; }
.input { width: 100%; height: 88rpx; background: #fff; border-radius: 16rpx; padding: 0 24rpx; margin-bottom: 20rpx; font-size: 28rpx; box-sizing: border-box; border: 1px solid #E5E7EB; }
.btn { width: 100%; height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #F59E0B, #F97316); color: #fff; border-radius: 44rpx; font-size: 30rpx; font-weight: 500; border: none; margin-bottom: 20rpx; }
.btn-wx { width: 100%; height: 88rpx; line-height: 88rpx; background: #07C160; color: #fff; border-radius: 44rpx; font-size: 30rpx; font-weight: 500; border: none; }
</style>
