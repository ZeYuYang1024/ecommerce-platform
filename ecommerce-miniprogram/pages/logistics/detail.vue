<template>
  <view class="tracking-page">
    <view class="header-card">
      <view class="provider-info">
        <text class="provider-name">{{ tracking.providerName || '物流公司' }}</text>
        <text class="tracking-no">运单号: {{ tracking.trackingNo }}</text>
      </view>
      <view class="status-tag" :class="'status-' + (tracking.shippingStatus || 0)">
        {{ tracking.shippingStatusText || '查询中' }}
      </view>
    </view>

    <view class="timeline" v-if="tracks.length > 0">
      <view class="timeline-item" v-for="(node, idx) in tracks" :key="idx"
        :class="{ active: idx === 0 }">
        <view class="dot" :class="{ active: idx === 0 }" />
        <view class="timeline-content">
          <text class="desc" :class="{ active: idx === 0 }">{{ node.desc }}</text>
          <text class="time">{{ formatTime(node.time) }}</text>
          <text class="location" v-if="node.location">{{ node.location }}</text>
        </view>
      </view>
    </view>

    <view class="empty" v-else-if="!loading">
      <image src="/static/empty_state.png" mode="aspectFit" class="empty-img" />
      <text class="empty-text">暂无物流轨迹</text>
    </view>

    <view class="loading" v-if="loading">
      <text>查询中...</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'

const props = defineProps({ shippingId: { type: [String, Number], required: true } })
const loading = ref(true)
const tracking = ref({})
const tracks = ref([])

const fetchTracking = async () => {
  loading.value = true
  try {
    const res = await request({ url: `/api/v1/logistics/tracking/shipping/${props.shippingId}` })
    if (res.code === 200 && res.data) {
      tracking.value = res.data
      tracks.value = res.data.tracks || []
    }
  } catch (e) {
    uni.showToast({ title: '查询失败', icon: 'none' })
  } finally { loading.value = false }
}

const formatTime = (time) => {
  if (!time) return ''
  return time.replace('T', ' ').substring(0, 19)
}

onMounted(fetchTracking)
</script>

<style scoped>
.tracking-page { min-height: 100vh; background: #f5f5f5; padding: 20rpx; }
.header-card { background: #fff; border-radius: 20rpx; padding: 30rpx; display: flex; justify-content: space-between; align-items: center; margin-bottom: 20rpx; }
.provider-name { font-size: 32rpx; font-weight: bold; color: #333; display: block; }
.tracking-no { font-size: 24rpx; color: #999; margin-top: 8rpx; display: block; }
.status-tag { padding: 10rpx 24rpx; border-radius: 30rpx; font-size: 24rpx; color: #fff; }
.status-1, .status-2, .status-3 { background: #3B82F6; }
.status-4 { background: #10B981; }
.status-5 { background: #EF4444; }
.timeline { padding: 30rpx; }
.timeline-item { display: flex; padding-bottom: 40rpx; position: relative; }
.timeline-item:not(:last-child)::before { content: ''; position: absolute; left: 11rpx; top: 24rpx; bottom: 0; width: 2rpx; background: #E5E7EB; }
.dot { width: 24rpx; height: 24rpx; border-radius: 50%; background: #D1D5DB; flex-shrink: 0; margin-right: 24rpx; margin-top: 4rpx; }
.dot.active { background: #3B82F6; }
.timeline-content { flex: 1; }
.desc { font-size: 28rpx; color: #666; display: block; }
.desc.active { color: #111; font-weight: bold; }
.time { font-size: 22rpx; color: #999; margin-top: 6rpx; display: block; }
.location { font-size: 22rpx; color: #999; margin-top: 2rpx; display: block; }
.empty { text-align: center; padding: 100rpx 0; }
.empty-img { width: 200rpx; height: 200rpx; }
.empty-text { color: #999; font-size: 28rpx; margin-top: 20rpx; }
.loading { text-align: center; padding: 100rpx; color: #999; }
</style>
