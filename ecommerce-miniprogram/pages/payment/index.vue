<template>
  <view class="page">
    <view v-if="loading" class="card state">正在查询订单…</view>
    <view v-else-if="error && !order" class="card state">
      <text>{{ error }}</text>
      <button class="secondary" @click="load">重试</button>
    </view>
    <view v-else-if="order" class="card">
      <text class="title">{{ paid ? '支付成功' : '订单支付' }}</text>
      <text class="muted">订单号 {{ order.orderNo }}</text>
      <text class="amount">¥{{ Number(order.totalAmount).toFixed(2) }}</text>
      <text v-if="!paid && payable" class="muted">演示环境使用模拟支付，无需付款。</text>
      <text v-else-if="!paid" class="muted">当前订单不可支付。</text>
      <text v-if="error" class="error" role="alert">{{ error }}</text>
      <button v-if="payable && !paid" class="primary" :disabled="submitting" @click="pay">
        {{ submitting ? '确认中…' : '确认模拟支付' }}
      </button>
      <button class="secondary" :disabled="submitting" @click="viewOrder">查看订单</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/utils/api'

const order = ref(null)
const loading = ref(true)
const submitting = ref(false)
const paid = ref(false)
const error = ref('')
const payable = computed(() => Number(order.value?.status) === 0)
let orderNo = ''

onLoad(options => {
  orderNo = options?.orderNo || ''
  void load()
})

async function checkPayment() {
  const result = await request({ url: `/api/v1/payment/orders/${encodeURIComponent(orderNo)}`, timeout: 5000 })
  return result.code === 200 && Number(result.data?.status) === 1
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    if (!orderNo) {
      error.value = '缺少订单号'
      return
    }
    const result = await request({ url: `/api/v1/orders/no/${encodeURIComponent(orderNo)}` })
    if (result.code !== 200 || !result.data) {
      error.value = result.message || '订单加载失败'
      return
    }
    order.value = result.data
    paid.value = [1, 2, 3].includes(Number(order.value.status)) || (payable.value && await checkPayment())
  } finally {
    loading.value = false
  }
}

async function pay() {
  if (!payable.value || paid.value || submitting.value) return
  submitting.value = true
  error.value = ''
  try {
    const result = await request({
      url: '/api/v1/payment/pay',
      method: 'POST',
      timeout: 10000,
      data: { orderNo, orderId: order.value.id, amount: order.value.totalAmount, payMethod: 'mock' }
    })
    paid.value = result.code === 200 || await checkPayment()
    if (!paid.value) {
      error.value = result.code === -1
        ? '暂未确认支付结果，可在订单详情核对后重试'
        : result.message || '支付失败，请刷新订单'
    }
  } finally {
    submitting.value = false
  }
}

function viewOrder() {
  const pages = getCurrentPages()
  if (pages.length > 1 && pages[pages.length - 2].route === 'pages/order-detail/index') {
    uni.navigateBack()
  } else {
    uni.redirectTo({ url: `/pages/order-detail/index?orderNo=${encodeURIComponent(orderNo)}` })
  }
}
</script>

<style scoped>
.page { padding: 24rpx; }
.card { padding: 32rpx; background: #fff; border-radius: 20rpx; }
.state, .card { text-align: center; }
.title, .muted, .amount, .error { display: block; }
.title { font-size: 36rpx; font-weight: 700; }
.muted { color: #5F6B7A; font-size: 26rpx; margin-top: 12rpx; }
.amount { color: #B91C1C; font-size: 52rpx; font-weight: 700; margin: 28rpx 0; }
.error { color: #B91C1C; font-size: 26rpx; margin-top: 18rpx; }
.primary, .secondary { border-radius: 20rpx; font-size: 28rpx; margin-top: 24rpx; }
.primary { background: #B45309; color: #fff; }
.secondary { background: #F3F4F6; color: #374151; }
.primary[disabled] { background: #E5E7EB; color: #5F6B7A; }
</style>
