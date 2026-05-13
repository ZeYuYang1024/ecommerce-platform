<template>
  <view class="page">
    <view v-if="sessions.length" v-for="s in sessions" :key="s.id" class="session">
      <view class="session-header">
        <text class="session-title">{{ s.name }}</text>
        <text class="countdown">距结束 {{ countdown(s.endTime) }}</text>
      </view>
      <view v-for="item in itemsBySession[s.id]" :key="item.id" class="seckill-card">
        <view class="card-left">
          <text class="price">¥{{ item.seckillPrice }}</text>
          <text class="original">¥{{ item.originalPrice }}</text>
        </view>
        <view class="card-mid">
          <text class="name">{{ item.name }}</text>
          <view class="progress-bar">
            <view class="progress-fill" :style="{width: progress(item)}"></view>
          </view>
          <text class="stock">剩余 {{ item.remainingCount }} 件</text>
        </view>
        <button class="btn-seckill" @click="doSeckill(item.id, item.remainingCount)">{{ item.remainingCount > 0 ? '秒杀' : '已抢光' }}</button>
      </view>
    </view>
    <view v-else class="empty">暂无秒杀活动</view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'

const sessions = ref([])
const itemsBySession = ref({})

onMounted(async () => {
  const res = await request({ url: '/api/v1/seckill/sessions?page=1&size=10' })
  if (res.code === 200) {
    sessions.value = res.data?.records || res.data || []
    for (const s of sessions.value) {
      const iRes = await request({ url: `/api/v1/seckill/items?sessionId=${s.id}&page=1&size=20` })
      if (iRes.code === 200) itemsBySession.value[s.id] = iRes.data?.records || iRes.data || []
    }
  }
})

function countdown(end) {
  const diff = new Date(end).getTime() - Date.now()
  if (diff <= 0) return '已结束'
  const h = Math.floor(diff / 3600000), m = Math.floor((diff % 3600000) / 60000), s = Math.floor((diff % 60000) / 1000)
  return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
}

function progress(item) { return `${((item.stockCount - item.remainingCount) / item.stockCount * 100).toFixed(0)}%` }

async function doSeckill(itemId, remaining) {
  if (remaining <= 0) return
  try {
    const res = await request({ url: '/api/v1/seckill/order', method: 'POST', data: { itemId, userId: 1 } })
    if (res.code === 200) uni.showToast({ title: '抢购成功！', icon: 'success' })
    else uni.showToast({ title: res.message, icon: 'none' })
  } catch {}
}
</script>

<style scoped>
.page { padding: 24rpx; }
.session { margin-bottom: 32rpx; }
.session-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.session-title { font-size: 34rpx; font-weight: bold; }
.countdown { font-size: 24rpx; color: #EF4444; background: #FEE2E2; padding: 6rpx 16rpx; border-radius: 20rpx; }
.seckill-card { display: flex; align-items: center; gap: 16rpx; background: #fff; border-radius: 20rpx; padding: 20rpx; margin-bottom: 12rpx; }
.card-left { text-align: center; min-width: 160rpx; }
.price { font-size: 40rpx; color: #EF4444; font-weight: bold; display: block; }
.original { font-size: 24rpx; color: #9CA3AF; text-decoration: line-through; }
.card-mid { flex: 1; }
.name { font-size: 28rpx; display: block; margin-bottom: 8rpx; }
.progress-bar { height: 8rpx; background: #F3F4F6; border-radius: 4rpx; margin-bottom: 4rpx; }
.progress-fill { height: 100%; background: #EF4444; border-radius: 4rpx; }
.stock { font-size: 22rpx; color: #9CA3AF; }
.btn-seckill { width: 140rpx; height: 72rpx; line-height: 72rpx; background: #EF4444; color: #fff; border-radius: 36rpx; font-size: 26rpx; border: none; }
.btn-seckill[disabled] { background: #D1D5DB; }
.empty { text-align: center; padding: 200rpx 0; color: #9CA3AF; }
</style>
