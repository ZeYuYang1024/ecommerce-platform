<template>
  <view class="page">
    <view class="header">
      <view class="avatar">U</view>
      <text class="nickname">{{ auth.isLogin ? (auth.userInfo && auth.userInfo.username) || '用户' : '未登录' }}</text>
      <button v-if="!auth.isLogin" class="btn-login" @click="login">登录</button>
    </view>

    <view class="order-bar">
      <text class="bar-title">我的订单</text>
      <view class="order-grid">
        <view v-for="o in orderLinks" :key="o.label" class="order-item" @click="goOrder">
          <text class="order-icon">{{ o.icon }}</text>
          <text>{{ o.label }}</text>
        </view>
      </view>
    </view>

    <view class="menu">
      <view v-for="m in menus" :key="m.label" class="menu-item" @click="m.action">
        <text>{{ m.label }}</text>
        <text class="arrow">→</text>
      </view>
    </view>

    <view v-if="auth.isLogin" class="menu">
      <view class="menu-item" @click="auth.logout">
        <text class="logout">退出登录</text>
      </view>
    </view>
  </view>
</template>

<script setup>
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()

const orderLinks = [
  { icon: '💳', label: '待付款' },
  { icon: '📦', label: '待发货' },
  { icon: '🚚', label: '待收货' },
  { icon: '✅', label: '已完成' },
]

const menus = [
  { label: '优惠券', action: () => uni.navigateTo({ url: '/pages/coupon/index' }) },
  { label: '收货地址', action: () => uni.navigateTo({ url: '/pages/address/index' }) },
  { label: '关于我们', action: () => uni.showToast({ title: '品质商城 v1.0', icon: 'none' }) },
]

function login() { uni.navigateTo({ url: '/pages/login/index' }) }
function goOrder() { uni.navigateTo({ url: '/pages/order/list' }) }
</script>

<style scoped>
.header { text-align: center; padding: 60rpx 0 40rpx; background: linear-gradient(135deg, #F59E0B, #F97316); }
.avatar { width: 120rpx; height: 120rpx; background: rgba(255,255,255,0.3); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48rpx; color: #fff; margin: 0 auto 16rpx; }
.nickname { font-size: 34rpx; color: #fff; font-weight: 600; display: block; }
.btn-login { margin-top: 16rpx; background: #fff; color: #F59E0B; border-radius: 32rpx; font-size: 26rpx; border: none; padding: 8rpx 48rpx; display: inline-block; }
.order-bar { background: #fff; border-radius: 20rpx; margin: -30rpx 24rpx 24rpx; padding: 24rpx; position: relative; z-index: 1; }
.bar-title { font-size: 30rpx; font-weight: 600; display: block; margin-bottom: 20rpx; }
.order-grid { display: flex; justify-content: space-around; }
.order-item { display: flex; flex-direction: column; align-items: center; font-size: 24rpx; gap: 8rpx; }
.order-icon { font-size: 40rpx; }
.menu { background: #fff; border-radius: 20rpx; margin: 0 24rpx 24rpx; overflow: hidden; }
.menu-item { display: flex; justify-content: space-between; padding: 28rpx 24rpx; font-size: 28rpx; border-bottom: 1px solid #F3F4F6; }
.menu-item:last-child { border-bottom: none; }
.arrow { color: #D1D5DB; }
.logout { color: #EF4444; }
</style>
