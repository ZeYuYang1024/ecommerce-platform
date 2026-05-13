<template>
  <view class="page">
    <view class="tabs">
      <view v-for="(t,i) in tabs" :key="i" :class="['tab', activeTab === i ? 'active' : '']" @click="switchTab(i)">{{ t }}</view>
    </view>

    <view v-if="orders.length" v-for="o in orders" :key="o.id" class="order-card" @click="goDetail(o.orderNo)">
      <view class="order-header">
        <text class="order-no">{{ o.orderNo }}</text>
        <text :class="['status', statusClass(o.status)]">{{ o.statusText }}</text>
      </view>
      <view v-for="item in o.items" :key="item.id" class="order-item">
        <image :src="getImageUrl(item.image)" mode="aspectFill" class="item-img" />
        <view class="item-info">
          <text class="item-name">{{ item.name }}</text>
          <text>¥{{ item.price }} × {{ item.quantity }}</text>
        </view>
      </view>
      <view class="order-footer">
        <text>共 {{ o.items ? o.items.length : 0 }} 件</text>
        <text class="amount">合计 ¥{{ o.totalAmount }}</text>
      </view>
    </view>
    <view v-else class="empty">
      <image src="/static/empty_order.png" mode="aspectFit" class="empty-img" />
      <text>暂无订单</text>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'
import { getImageUrl } from '@/utils/image'

const tabs = ['全部','待付款','待发货','待收货','已完成']
const activeTab = ref(0)
const orders = ref([])

onMounted(() => fetchOrders())

async function fetchOrders() {
  const res = await request({ url: '/api/v1/orders' })
  if (res.code === 200) orders.value = res.data?.records || []
}

function switchTab(i) { activeTab.value = i }
function statusClass(s) { return { 0: 'amber', 1: 'green', 2: 'blue', 3: 'gray', 4: 'red' }[s] || '' }
function goDetail(orderNo) { uni.navigateTo({ url: `/pages/order-detail/index?orderNo=${orderNo}` }) }
</script>

<style scoped>
.page { padding: 24rpx; }
.tabs { display: flex; background: #fff; border-radius: 16rpx; margin-bottom: 24rpx; }
.tab { flex: 1; text-align: center; padding: 20rpx; font-size: 26rpx; color: #6B7280; }
.tab.active { color: #F59E0B; font-weight: 600; }
.order-card { background: #fff; border-radius: 20rpx; padding: 20rpx; margin-bottom: 16rpx; }
.order-header { display: flex; justify-content: space-between; margin-bottom: 16rpx; }
.order-no { font-size: 24rpx; color: #9CA3AF; }
.status { font-size: 24rpx; }
.status.amber { color: #F59E0B; } .status.green { color: #10B981; } .status.blue { color: #3B82F6; } .status.gray { color: #9CA3AF; } .status.red { color: #EF4444; }
.order-item { display: flex; gap: 12rpx; padding: 12rpx 0; border-top: 1px solid #F3F4F6; }
.item-img { width: 100rpx; height: 100rpx; border-radius: 12rpx; background: #F8F8F8; }
.item-info { flex: 1; font-size: 24rpx; }
.item-name { font-size: 26rpx; display: block; margin-bottom: 4rpx; }
.order-footer { display: flex; justify-content: space-between; padding-top: 12rpx; border-top: 1px solid #F3F4F6; font-size: 26rpx; }
.amount { font-weight: bold; }
.empty { text-align: center; padding: 150rpx 0; color: #9CA3AF; }
.empty-img { width: 300rpx; height: 200rpx; margin-bottom: 24rpx; }
</style>
