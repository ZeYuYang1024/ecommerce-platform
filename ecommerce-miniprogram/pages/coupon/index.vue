<template>
  <view class="page">
    <view class="tabs">
      <view :class="['tab', tab === 0 ? 'active' : '']" @click="tab = 0">可领取</view>
      <view :class="['tab', tab === 1 ? 'active' : '']" @click="tab = 1">我的券</view>
    </view>

    <view v-if="tab === 0">
      <view v-for="t in templates" :key="t.id" class="coupon-card">
        <view class="card-left">
          <text class="amount">{{ discountText(t) }}</text>
          <text class="condition">{{ conditionText(t) }}</text>
        </view>
        <view class="card-right">
          <text class="name">{{ t.name }}</text>
          <text class="valid">有效期至 {{ t.endTime ? t.endTime.substring(0, 10) : '' }}</text>
          <button class="btn-claim" @click="claim(t.id)">立即领取</button>
        </view>
      </view>
      <view v-if="!templates.length" class="empty">暂无可领取的优惠券</view>
    </view>

    <view v-else>
      <view v-for="c in myCoupons" :key="c.userCouponId" class="coupon-card" :class="{ used: c.status !== 0 }">
        <view class="card-left">
          <text class="amount">{{ discountText(c) }}</text>
        </view>
        <view class="card-right">
          <text class="name">{{ c.name }}</text>
          <text :class="['status', c.status === 0 ? 'active' : '']">{{ ['可使用','已使用','已过期','不可使用'][c.status] || '' }}</text>
        </view>
      </view>
      <view v-if="!myCoupons.length" class="empty">暂无优惠券</view>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onShow, onReachBottom } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const tab = ref(0)
const templates = ref([])
const myCoupons = ref([])

const pages = [0, 0]
const totals = [0, 0]
const busy = [false, false]
const size = 20

async function load(kind, reset = false) {
  const list = kind === 0 ? templates : myCoupons
  if (kind === 1 && !auth.isLogin) {
    list.value = []
    return
  }
  if (busy[kind] || (!reset && list.value.length >= totals[kind])) return
  busy[kind] = true
  try {
    const page = reset ? 1 : pages[kind] + 1
    const route = kind === 0 ? 'available' : 'mine'
    const res = await request({ url: `/api/v1/coupons/${route}?page=${page}&size=${size}`, auth: kind === 1 })
    if (res.code !== 200) { uni.showToast({ title: res.message || '优惠券加载失败', icon: 'none' }); return }
    const rows = res.data?.records || []
    list.value = reset ? rows : [...list.value, ...rows]
    pages[kind] = page
    totals[kind] = Number(res.data?.total || 0)
  } finally {
    busy[kind] = false
  }
}

onShow(() => Promise.all([load(0, true), load(1, true)]))
onReachBottom(() => load(tab.value))

function discountText(c) {
  if (c.type === 'FLAT') return `¥${c.discountAmount}`
  if (c.type === 'DISCOUNT') return `${Number((c.discountRate * 10).toFixed(2))}折`
  return `¥${c.discountAmount}`
}
function conditionText(t) { return t.minAmount > 0 ? `满${t.minAmount}可用` : '无门槛' }

async function claim(templateId) {
  if (!auth.isLogin) { uni.showToast({ title: '请先登录', icon: 'none' }); return }
  const res = await request({ url: `/api/v1/coupons/claim?templateId=${templateId}`, method: 'POST' })
  if (res.code === 200) {
    uni.showToast({ title: '领取成功', icon: 'success' })
    tab.value = 1
    await Promise.all([load(0, true), load(1, true)])
  }
  else uni.showToast({ title: res.message, icon: 'none' })
}
</script>

<style scoped>
.page { padding: 24rpx; }
.tabs { display: flex; margin-bottom: 24rpx; background: #fff; border-radius: 16rpx; overflow: hidden; }
.tab { flex: 1; text-align: center; padding: 20rpx; font-size: 28rpx; color: #6B7280; }
.tab.active { color: #F59E0B; font-weight: 600; border-bottom: 4rpx solid #F59E0B; }
.coupon-card { display: flex; background: #fff; border-radius: 20rpx; overflow: hidden; margin-bottom: 16rpx; }
.coupon-card.used { opacity: 0.5; }
.card-left { width: 200rpx; background: linear-gradient(135deg, #FEF3C7, #FDE68A); padding: 24rpx; display: flex; flex-direction: column; align-items: center; justify-content: center; }
.amount { font-size: 40rpx; font-weight: bold; color: #D97706; }
.condition { font-size: 22rpx; color: #92400E; margin-top: 4rpx; }
.card-right { flex: 1; padding: 24rpx; }
.name { font-size: 28rpx; display: block; margin-bottom: 8rpx; }
.valid { font-size: 22rpx; color: #9CA3AF; display: block; }
.status { font-size: 22rpx; color: #9CA3AF; }
.status.active { color: #10B981; }
.btn-claim { margin-top: 12rpx; height: 56rpx; line-height: 56rpx; background: #F59E0B; color: #fff; border-radius: 28rpx; font-size: 24rpx; border: none; padding: 0 24rpx; }
.empty { text-align: center; padding: 100rpx 0; color: #9CA3AF; }
</style>
