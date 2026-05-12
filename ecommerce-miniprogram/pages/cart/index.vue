<template>
  <view class="page">
    <view v-if="cartStore.items.length" class="cart-list">
      <view v-for="item in cartStore.items" :key="item.id" class="cart-item">
        <view :class="['checkbox', item.checked ? 'checked' : '']" @click="cartStore.toggleCheck(item.id)">✓</view>
        <image :src="item.image || '/static/product_01.png'" mode="aspectFill" class="item-img" />
        <view class="item-info">
          <text class="item-name">{{ item.name }}</text>
          <text class="text-price">¥{{ item.price }}</text>
          <view class="qty-row">
            <view class="qty-btn" @click="cartStore.updateQuantity(item.id, item.quantity - 1)">-</view>
            <text>{{ item.quantity }}</text>
            <view class="qty-btn" @click="cartStore.updateQuantity(item.id, item.quantity + 1)">+</view>
          </view>
        </view>
      </view>
    </view>
    <view v-else class="empty">
      <image src="/static/empty_cart.png" mode="aspectFit" class="empty-img" />
      <text>购物车空空如也</text>
    </view>

    <view v-if="cartStore.items.length" class="bottom-bar">
      <view class="check-all" @click="cartStore.toggleAll">全选</view>
      <text class="total">合计 ¥{{ cartStore.checkedAmount.toFixed(2) }}</text>
      <button class="btn-primary" @click="checkout" :disabled="!cartStore.checkedItems.length">去结算</button>
    </view>
  </view>
</template>

<script setup>
import { onMounted } from 'vue'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()
onMounted(() => cartStore.fetchCart())

function checkout() {
  uni.navigateTo({ url: '/pages/checkout/index' })
}
</script>

<style scoped>
.page { padding-bottom: 120rpx; }
.cart-list { padding: 24rpx; }
.cart-item { display: flex; align-items: center; gap: 16rpx; background: #fff; border-radius: 20rpx; padding: 20rpx; margin-bottom: 16rpx; }
.checkbox { width: 40rpx; height: 40rpx; border: 2rpx solid #D1D5DB; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 24rpx; color: transparent; }
.checkbox.checked { background: #F59E0B; border-color: #F59E0B; color: #fff; }
.item-img { width: 140rpx; height: 140rpx; border-radius: 12rpx; background: #F8F8F8; }
.item-info { flex: 1; }
.item-name { font-size: 28rpx; display: block; margin-bottom: 8rpx; }
.qty-row { display: flex; align-items: center; gap: 20rpx; margin-top: 12rpx; }
.qty-btn { width: 48rpx; height: 48rpx; background: #F3F4F6; border-radius: 50%; display: flex; align-items: center; justify-content: center; font-size: 32rpx; color: #6B7280; }
.empty { text-align: center; padding: 200rpx 0; color: #9CA3AF; }
.empty-img { width: 300rpx; height: 200rpx; margin-bottom: 24rpx; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; gap: 16rpx; }
.check-all { font-size: 26rpx; color: #6B7280; }
.total { flex: 1; font-size: 30rpx; font-weight: bold; }
.bottom-bar .btn-primary { height: 72rpx; line-height: 72rpx; padding: 0 40rpx; border-radius: 36rpx; }
</style>
