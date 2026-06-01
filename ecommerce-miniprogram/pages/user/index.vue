<template>
  <view class="page">
    <view class="header">
      <view class="avatar">U</view>
      <text class="nickname">{{ auth.isLogin ? (auth.userInfo && auth.userInfo.username) || '用户' : '未登录' }}</text>
      <button v-if="!auth.isLogin" class="btn-login" @click="login">登录</button>

      <view v-if="auth.isLogin" class="member-card">
        <view class="member-row">
          <view class="member-level">
            <text class="level-icon">{{ levelIcon }}</text>
            <text class="level-name">{{ member.level ? member.level.name : '普通会员' }}</text>
          </view>
          <view class="member-points">
            <text class="points-num">{{ member.availablePoints || 0 }}</text>
            <text class="points-label">积分</text>
          </view>
        </view>
        <view v-if="member.nextLevelGrowth" class="growth-bar">
          <text class="growth-text">成长值 {{ member.growthValue || 0 }}/{{ member.nextLevelGrowth }}</text>
          <progress :percent="progressPercent" stroke-width="6" activeColor="#F59E0B" backgroundColor="#FDE68A" border-radius="4" />
        </view>
      </view>
    </view>

    <view v-if="auth.isLogin" class="checkin-bar" @click="doCheckIn">
      <text>{{ checkedIn ? '今日已签到' : '每日签到领积分' }}</text>
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
import { ref, computed } from 'vue'
import { useAuthStore } from '@/stores/auth'
import { request } from '@/utils/api'

const auth = useAuthStore()

const member = ref({ level: null, growthValue: 0, nextLevelGrowth: null, availablePoints: 0 })
const checkedIn = ref(false)

const orderLinks = [
  { icon: '💳', label: '待付款' },
  { icon: '📦', label: '待发货' },
  { icon: '🚚', label: '待收货' },
  { icon: '✅', label: '已完成' }
]

const menus = [
  { label: '积分明细', action: () => uni.showToast({ title: '积分明细页开发中', icon: 'none' }) },
  { label: '优惠券', action: () => uni.navigateTo({ url: '/pages/coupon/index' }) },
  { label: '收货地址', action: () => uni.navigateTo({ url: '/pages/address/index' }) },
  { label: '关于我们', action: () => uni.showToast({ title: '品质商城 v1.0', icon: 'none' }) }
]

const levelIcon = computed(() => {
  const map = { REGULAR: '🥉', SILVER: '🥈', GOLD: '🥇', DIAMOND: '💎' }
  return map[(member.value.level && member.value.level.levelCode) || ''] || '🥉'
})

const progressPercent = computed(() => {
  if (!member.value.nextLevelGrowth) return 100
  return Math.min(100, Math.round(((member.value.growthValue || 0) / member.value.nextLevelGrowth) * 100))
})

function login() {
  uni.navigateTo({ url: '/pages/login/index' })
}

function goOrder() {
  uni.navigateTo({ url: '/pages/order/list' })
}

async function fetchMember() {
  try {
    const res = await request({ url: '/api/v1/member/profile', method: 'GET' })
    if (res.code === 200) {
      member.value = res.data || member.value
    }
  } catch (e) {
    // ignore
  }
}

async function fetchCheckInStatus() {
  try {
    const res = await request({ url: '/api/v1/member/check-in/status', method: 'GET' })
    if (res.code === 200) {
      checkedIn.value = res.data?.checkedToday || false
    }
  } catch (e) {
    // ignore
  }
}

async function doCheckIn() {
  if (checkedIn.value) return
  try {
    const res = await request({ url: '/api/v1/member/check-in', method: 'POST' })
    if (res.code === 200) {
      checkedIn.value = true
      fetchMember()
      uni.showToast({ title: `签到成功 +${res.data.pointsAwardedToday}积分`, icon: 'none' })
    }
  } catch (e) {
    // ignore
  }
}

if (auth.isLogin) {
  fetchMember()
  fetchCheckInStatus()
}
</script>

<style scoped>
.header { text-align: center; padding: 60rpx 0 20rpx; background: linear-gradient(135deg, #F59E0B, #F97316); }
.avatar { width: 120rpx; height: 120rpx; background: rgba(255,255,255,0.3); border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 48rpx; color: #fff; margin: 0 auto 16rpx; }
.nickname { font-size: 34rpx; color: #fff; font-weight: 600; display: block; }
.btn-login { margin-top: 16rpx; background: #fff; color: #F59E0B; border-radius: 32rpx; font-size: 26rpx; border: none; padding: 8rpx 48rpx; display: inline-block; }

.member-card { background: rgba(255,255,255,0.2); border-radius: 16rpx; margin: 20rpx 32rpx 0; padding: 20rpx; }
.member-row { display: flex; justify-content: space-between; align-items: center; }
.member-level { display: flex; align-items: center; gap: 8rpx; }
.level-icon { font-size: 36rpx; }
.level-name { font-size: 26rpx; color: #fff; }
.member-points { text-align: right; }
.points-num { font-size: 40rpx; color: #fff; font-weight: 700; display: block; }
.points-label { font-size: 20rpx; color: rgba(255,255,255,0.7); }
.growth-bar { margin-top: 16rpx; }
.growth-text { font-size: 22rpx; color: rgba(255,255,255,0.8); margin-bottom: 8rpx; display: block; }

.checkin-bar { background: #fff; border-radius: 20rpx; margin: -20rpx 24rpx 24rpx; padding: 24rpx; text-align: center; font-size: 28rpx; color: #F59E0B; font-weight: 600; position: relative; z-index: 1; }
.checkin-bar:active { background: #FEF3C7; }

.order-bar { background: #fff; border-radius: 20rpx; margin: 0 24rpx 24rpx; padding: 24rpx; }
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
