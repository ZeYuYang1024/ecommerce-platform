<template>
  <view class="page">
    <!-- Address -->
    <view class="card">
      <text class="label">收货地址</text>
      <text class="addr">请选择收货地址</text>
    </view>

    <!-- Items -->
    <view class="card">
      <text class="label">商品清单</text>
      <view v-for="item in items" :key="item.id" class="item-row">
        <image :src="getImageUrl(item.image)" mode="aspectFill" class="item-img" />
        <view class="item-info">
          <text class="item-name">{{ item.name }}</text>
          <text class="text-price">¥{{ item.price }} × {{ item.quantity }}</text>
        </view>
      </view>
    </view>

    <!-- Coupon -->
    <view class="card" @click="showCoupons = !showCoupons">
      <view class="row-between">
        <text class="label">优惠券</text>
        <text class="value">{{ selectedCoupon ? '¥' + discount + ' 已选' : '选择优惠券' }}</text>
      </view>
      <view v-if="showCoupons" class="coupon-list">
        <view v-for="c in availableCoupons" :key="c.userCouponId" class="coupon-item" @click="selectCoupon(c)">
          <text>{{ c.name }} ({{ discountText(c) }})</text>
        </view>
        <text v-if="!availableCoupons.length" class="text-muted">暂无可用优惠券</text>
      </view>
    </view>

    <!-- Total -->
    <view class="card">
      <view class="row-between">
        <text>商品金额</text>
        <text>¥{{ totalAmount }}</text>
      </view>
      <view class="row-between" v-if="discount > 0">
        <text>优惠券</text>
        <text class="discount">-¥{{ discount }}</text>
      </view>
      <view class="row-between">
        <text>运费</text>
        <text v-if="shippingFee > 0" class="shipping-fee">¥{{ shippingFee.toFixed(2) }}</text>
        <text v-else class="free-shipping">包邮</text>
      </view>
      <view class="row-between total-row">
        <text>应付</text>
        <text class="final-price">¥{{ (totalAmount - discount + shippingFee).toFixed(2) }}</text>
      </view>
    </view>

    <!-- Submit -->
    <view class="submit-bar">
      <text class="final">合计 ¥{{ (totalAmount - discount + shippingFee).toFixed(2) }}</text>
      <button class="btn-primary" @click="submitOrder">提交订单</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'
import { useCartStore } from '@/stores/cart'
import { getImageUrl } from '@/utils/image'

const cartStore = useCartStore()
const items = ref([])
const totalAmount = ref(0)
const availableCoupons = ref([])
const selectedCoupon = ref(null)
const discount = ref(0)
const showCoupons = ref(false)
const shippingFee = ref(0)

onMounted(async () => {
  await cartStore.fetchCart()
  items.value = cartStore.checkedItems
  totalAmount.value = cartStore.checkedAmount

  const res = await request({ url: '/api/v1/coupons?status=0' })
  if (res.code === 200) availableCoupons.value = res.data || []

  // Calculate shipping fee
  try {
    const totalQty = items.value.reduce((sum, i) => sum + (i.quantity || 0), 0)
    const feeRes = await request({
      url: '/api/v1/internal/logistics/shipping-fee',
      method: 'POST',
      data: { templateId: 1, quantity: totalQty, weight: 0, volume: 0, provinceCode: '' }
    })
    if (feeRes.code === 200) shippingFee.value = feeRes.data?.shippingFee || 0
  } catch (e) { /* keep default 0 */ }
})

function discountText(c) { return c.type === 'FLAT' ? `立减¥${c.discountAmount}` : `¥${c.discountAmount}` }

async function selectCoupon(c) {
  if (selectedCoupon.value?.userCouponId === c.userCouponId) { selectedCoupon.value = null; discount.value = 0; return }
  const res = await request({ url: '/api/v1/internal/coupons/verify', method: 'POST', data: { userCouponId: c.userCouponId, userId: 1, orderAmount: totalAmount.value } })
  if (res.code === 200) { selectedCoupon.value = c; discount.value = res.data?.discount || 0 }
  else uni.showToast({ title: res.message, icon: 'none' })
}

async function submitOrder() {
  const res = await request({ url: '/api/v1/orders', method: 'POST', data: { items: items.value.map(i => ({ skuId: i.skuId, quantity: i.quantity })) } })
  if (res.code === 200) {
    // Use coupon if selected
    if (selectedCoupon.value) {
      await request({ url: '/api/v1/internal/coupons/use', method: 'POST', data: { userCouponId: selectedCoupon.value.userCouponId, orderNo: res.data?.orderNo } })
    }
    cartStore.removeCheckedItems()
    uni.redirectTo({ url: `/pages/order-detail/index?orderNo=${res.data?.orderNo}` })
  }
}
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 140rpx; }
.card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 16rpx; }
.label { font-size: 28rpx; font-weight: 600; display: block; margin-bottom: 12rpx; }
.addr { font-size: 26rpx; color: #9CA3AF; }
.item-row { display: flex; gap: 16rpx; margin-top: 12rpx; }
.item-img { width: 100rpx; height: 100rpx; border-radius: 12rpx; background: #F8F8F8; }
.item-info { flex: 1; }
.item-name { font-size: 26rpx; }
.row-between { display: flex; justify-content: space-between; align-items: center; padding: 8rpx 0; font-size: 26rpx; }
.value { color: #F59E0B; }
.total-row { border-top: 1px solid #E5E7EB; margin-top: 8rpx; padding-top: 16rpx; font-size: 30rpx; font-weight: bold; }
.final-price { color: #EF4444; font-size: 36rpx; }
.discount { color: #EF4444; }
.shipping-fee { color: #333; }
.free-shipping { color: #10B981; font-weight: 600; }
.coupon-list { margin-top: 12rpx; }
.coupon-item { padding: 16rpx; background: #FEF3C7; border-radius: 12rpx; margin-top: 8rpx; font-size: 26rpx; }
.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; gap: 16rpx; }
.final { font-size: 28rpx; font-weight: bold; flex: 1; }
.submit-bar .btn-primary { height: 80rpx; line-height: 80rpx; padding: 0 48rpx; border-radius: 40rpx; }
</style>
