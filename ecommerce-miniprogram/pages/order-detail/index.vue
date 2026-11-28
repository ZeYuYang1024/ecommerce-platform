<template>
  <view v-if="loading" class="page text-muted">加载订单中…</view>
  <view v-else-if="!order" class="page">
    <text>{{ loadError || '订单不存在' }}</text>
    <button class="btn-cancel" @click="load">重试</button>
  </view>
  <view v-else class="page">
    <view class="card">
      <text class="status-text">{{ orderStatuses[order.status] || order.statusText }}</text>
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
        <ProductImage :source="item.image" width="80rpx" height="80rpx" radius="8rpx" />
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
    <view v-if="afterSale?.application || afterSale?.canApply" class="card">
      <text class="label">售后</text>
      <template v-if="afterSale.application">
        <text class="after-sale-status">{{ afterSaleStatuses[afterSale.application.status] }}</text>
        <text class="text-muted">退款金额 ¥{{ afterSale.application.amount }}</text>
        <text class="text-muted">申请原因 {{ afterSale.application.reason }}</text>
        <text v-if="afterSale.application.reviewNote" class="text-muted">审核说明 {{ afterSale.application.reviewNote }}</text>
        <text v-if="afterSale.application.failureReason" class="text-muted">{{ afterSale.application.failureReason }}</text>
        <text v-if="afterSale.application.refundNo" class="text-muted">退款单号 {{ afterSale.application.refundNo }}</text>
        <button v-if="['PENDING', 'REFUNDING'].includes(afterSale.application.status)" class="btn-cancel" @click="loadAfterSale">刷新进度</button>
      </template>
      <template v-else-if="applying">
        <text class="text-muted">申请全额退款 ¥{{ order.totalAmount }}</text>
        <textarea v-model="reason" class="reason" placeholder="填写退款原因" maxlength="256" />
        <button class="btn-primary" :disabled="submitting" @click="submitAfterSale">提交退款申请</button>
        <button class="btn-cancel" :disabled="submitting" @click="applying = false">返回</button>
      </template>
      <button v-else class="btn-cancel" @click="applying = true">申请全额退款</button>
    </view>
    <view v-else-if="afterSaleError" class="card">
      <text class="text-muted">{{ afterSaleError }}</text>
      <button class="btn-cancel" @click="loadAfterSale">重试售后查询</button>
    </view>
    <text v-if="actionError" class="error" role="alert">{{ actionError }}</text>
    <view v-if="order.status === 0" class="actions">
      <button class="btn-primary" @click="pay">去支付</button>
      <button class="btn-cancel" @click="cancel">取消订单</button>
    </view>
    <view v-if="[2, 3].includes(order.status)" class="actions">
      <button class="btn-primary" @click="viewLogistics">查看物流</button>
    </view>
  </view>
</template>

<script setup>
import { ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import ProductImage from '@/components/ProductImage.vue'

const order = ref(null)
const afterSale = ref(null)
const loading = ref(true)
const loadError = ref('')
const afterSaleError = ref('')
const applying = ref(false)
const submitting = ref(false)
const reason = ref('')
const actionError = ref('')
const orderStatuses = ['待支付', '已支付', '已发货', '已完成', '已取消', '已退款']
const afterSaleStatuses = {
  PENDING: '待审核', REJECTED: '已拒绝', REFUNDING: '退款处理中', REFUNDED: '已退款', FAILED: '退款失败'
}
let orderNo = ''

onLoad(options => { orderNo = options?.orderNo || '' })
onShow(load)

async function load() {
  loading.value = true
  loadError.value = ''
  try {
    if (!orderNo) return
    const res = await request({ url: `/api/v1/orders/no/${encodeURIComponent(orderNo)}` })
    order.value = res.code === 200 ? res.data : null
    if (order.value) await loadAfterSale()
    else loadError.value = res.message || '订单加载失败'
  } finally {
    loading.value = false
  }
}

function afterSaleUrl() { return `/api/v1/orders/no/${encodeURIComponent(orderNo)}/after-sale` }

async function loadAfterSale() {
  afterSaleError.value = ''
  const result = await request({ url: afterSaleUrl() })
  if (result.code === 200) {
    afterSale.value = result.data
    if (result.data.application?.status === 'REFUNDED') order.value.status = 5
  } else {
    afterSale.value = null
    afterSaleError.value = result.message || '售后状态加载失败'
  }
}

async function submitAfterSale() {
  if (submitting.value) return
  const value = reason.value.trim()
  if (!value) {
    actionError.value = '请填写退款原因'
    return
  }
  submitting.value = true
  actionError.value = ''
  try {
    const result = await request({ url: afterSaleUrl(), method: 'POST', data: { reason: value } })
    if (result.code === 200) {
      afterSale.value = { canApply: false, application: result.data }
      applying.value = false
      reason.value = ''
    } else {
      actionError.value = result.message || '提交退款申请失败'
      await loadAfterSale()
    }
  } finally { submitting.value = false }
}

function pay() { uni.navigateTo({ url: `/pages/payment/index?orderNo=${encodeURIComponent(orderNo)}` }) }
async function cancel() {
  const res = await request({ url: `/api/v1/orders/${order.value.id}/cancel`, method: 'PUT' })
  if (res.code === 200) { order.value.status = 4; order.value.statusText = '已取消' }
}

async function viewLogistics() {
  const res = await request({ url: `/api/v1/internal/logistics/shipping/by-order/${order.value.id}` })
  if (res.code === 200 && res.data) {
    uni.navigateTo({ url: `/pages/logistics/detail?shippingId=${res.data.id}` })
  } else {
    uni.showToast({ title: '暂无物流信息', icon: 'none' })
  }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 16rpx; }
.status-text { font-size: 32rpx; font-weight: bold; color: #F59E0B; display: block; margin-bottom: 8rpx; }
.order-no { font-size: 24rpx; color: #9CA3AF; }
.label { font-size: 28rpx; font-weight: 600; display: block; margin-bottom: 12rpx; }
.item-row { display: flex; gap: 12rpx; padding: 12rpx 0; border-top: 1px solid #F3F4F6; align-items: center; }
.item-info { flex: 1; font-size: 26rpx; }
.item-total { font-size: 26rpx; font-weight: 500; }
.total-row { display: flex; justify-content: space-between; padding-top: 16rpx; border-top: 1px solid #F3F4F6; font-size: 30rpx; font-weight: bold; margin-top: 8rpx; }
.amount { color: #EF4444; }
.actions { display: flex; gap: 16rpx; position: fixed; bottom: 0; left: 0; right: 0; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; }
.btn-cancel { flex: 1; background: #F3F4F6; color: #6B7280; border-radius: 40rpx; font-size: 28rpx; border: none; height: 80rpx; line-height: 80rpx; }
.after-sale-status { font-size: 30rpx; font-weight: 600; display: block; }
.text-muted { display: block; margin-top: 8rpx; overflow-wrap: anywhere; }
.reason { width: 100%; min-height: 140rpx; box-sizing: border-box; padding: 16rpx; margin: 16rpx 0; background: #F3F4F6; border-radius: 12rpx; }
.error { display: block; color: #B91C1C; font-size: 26rpx; }
</style>
