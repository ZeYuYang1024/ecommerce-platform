<template>
  <view class="page" v-if="order">
    <view class="card">
      <text class="status-text">{{ order.statusText }}</text>
      <text class="order-no">订单号 {{ order.orderNo }}</text>
    </view>
    <view class="card">
      <text class="label">收货信息</text>
      <text>{{ order.receiverName }} {{ order.receiverPhone }}</text>
      <text class="text-muted">{{ order.receiverAddress }}</text>
    </view>
    <view class="card">
      <text class="label">商品明细</text>
      <view v-for="item in order.items" :key="item.id" class="item-row">
        <image :src="getImageUrl(item.image)" mode="aspectFill" class="item-img" />
        <view class="item-info">
          <text>{{ item.name }}</text>
          <text class="text-muted">¥{{ item.price }} × {{ item.quantity }}</text>
        </view>
        <text class="item-total">¥{{ item.totalPrice }}</text>
      </view>
      <view class="total-row">
        <text>合计</text>
        <text class="amount">¥{{ order.totalAmount }}</text>
      </view>
    </view>
    <view v-if="order.status === 0" class="actions">
      <button class="btn-primary" @click="pay">去支付</button>
      <button class="btn-cancel" @click="cancel">取消订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import { getImageUrl } from '@/utils/image'

const order = ref(null)

onLoad(async (options) => {
  if (!options?.orderNo) return
  const res = await request({ url: `/api/v1/orders/no/${options.orderNo}` })
  if (res.code === 200) order.value = res.data
})

async function pay() { uni.navigateTo({ url: `/pages/payment/index?orderNo=${order.value.orderNo}` }) }
async function cancel() {
  const res = await request({ url: `/api/v1/orders/${order.value.id}/cancel`, method: 'PUT' })
  if (res.code === 200) { order.value.status = 4; order.value.statusText = '已取消' }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 16rpx; }
.status-text { font-size: 32rpx; font-weight: bold; color: #F59E0B; display: block; margin-bottom: 8rpx; }
.order-no { font-size: 24rpx; color: #9CA3AF; }
.label { font-size: 28rpx; font-weight: 600; display: block; margin-bottom: 12rpx; }
.item-row { display: flex; gap: 12rpx; padding: 12rpx 0; border-top: 1px solid #F3F4F6; align-items: center; }
.item-img { width: 80rpx; height: 80rpx; border-radius: 8rpx; background: #F8F8F8; }
.item-info { flex: 1; font-size: 26rpx; }
.item-total { font-size: 26rpx; font-weight: 500; }
.total-row { display: flex; justify-content: space-between; padding-top: 16rpx; border-top: 1px solid #F3F4F6; font-size: 30rpx; font-weight: bold; margin-top: 8rpx; }
.amount { color: #EF4444; }
.actions { display: flex; gap: 16rpx; position: fixed; bottom: 0; left: 0; right: 0; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; }
.btn-cancel { flex: 1; background: #F3F4F6; color: #6B7280; border-radius: 40rpx; font-size: 28rpx; border: none; height: 80rpx; line-height: 80rpx; }
</style>
