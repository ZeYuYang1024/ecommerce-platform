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
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { useAuthStore } from '@/stores/auth'

const username = ref('')
const password = ref('')
const loading = ref(false)
const auth = useAuthStore()

async function doLogin() {
  if (loading.value) return
  if (!username.value.trim() || !password.value) {
    uni.showToast({ title: '请输入用户名和密码', icon: 'none' })
    return
  }
  loading.value = true
  try {
    const res = await auth.login(username.value.trim(), password.value)
    if (res.code === 200 && auth.isLogin) {
      if (getCurrentPages().length > 1) uni.navigateBack()
      else uni.switchTab({ url: '/pages/index/index' })
    } else {
      uni.showToast({ title: res.message || '登录失败', icon: 'none' })
    }
  } catch {
    uni.showToast({ title: '登录失败', icon: 'none' })
  } finally {
    loading.value = false
  }
}

</script>

<style>
.page { min-height: 100vh; box-sizing: border-box; display: flex; flex-direction: column; align-items: center; justify-content: center; padding: 60rpx 48rpx; background: #F8F8F8; }
.card { text-align: center; margin-bottom: 60rpx; }
.logo { width: 120rpx; height: 120rpx; background: linear-gradient(135deg, #F59E0B, #F97316); border-radius: 30rpx; display: flex; align-items: center; justify-content: center; font-size: 56rpx; color: #fff; margin: 0 auto 24rpx; }
.title { font-size: 40rpx; font-weight: bold; display: block; color: #1F2937; }
.subtitle { font-size: 26rpx; color: #9CA3AF; display: block; margin-top: 8rpx; }
.form { width: 100%; max-width: 600rpx; }
.input { width: 100%; height: 88rpx; background: #fff; border-radius: 16rpx; padding: 0 24rpx; margin-bottom: 20rpx; font-size: 28rpx; box-sizing: border-box; border: 1px solid #E5E7EB; }
.btn { width: 100%; height: 88rpx; line-height: 88rpx; background: linear-gradient(135deg, #F59E0B, #F97316); color: #fff; border-radius: 44rpx; font-size: 30rpx; font-weight: 500; border: none; margin-bottom: 20rpx; }
</style>
