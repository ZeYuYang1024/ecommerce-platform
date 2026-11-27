<template>
  <view class="page">
    <view v-if="!auth.isLogin" class="empty">
      <text>登录后查看购物车</text>
      <button class="entry-button" @click="goLogin">立即登录</button>
    </view>
    <view v-else-if="loading" class="empty">加载中…</view>
    <view v-else-if="error" class="empty">
      <text>{{ error }}</text>
      <button class="entry-button" @click="loadCart">重试</button>
    </view>
    <view v-else-if="cartStore.items.length" class="cart-list">
      <view v-for="item in cartStore.items" :key="item.skuId" class="cart-item">
        <button
          :class="['checkbox', item.checked && isPurchasable(item) ? 'checked' : '']"
          :disabled="busy || !isPurchasable(item)"
          aria-label="选择商品"
          @click="update(() => cartStore.toggleCheck(item.skuId))"
        >{{ item.checked && isPurchasable(item) ? '✓' : '○' }}</button>
        <ProductImage :source="item.image" width="128rpx" height="128rpx" radius="12rpx" />
        <view class="item-info">
          <text class="item-name">{{ item.name || '商品信息暂不可用' }}</text>
          <template v-if="isPurchasable(item)">
            <text class="text-price">¥{{ Number(item.price).toFixed(2) }}</text>
            <view class="qty-row">
              <button class="qty-btn" :disabled="busy" aria-label="减少数量" @click="update(() => cartStore.updateQuantity(item.skuId, item.quantity - 1))">−</button>
              <text>{{ item.quantity }}</text>
              <button class="qty-btn" :disabled="busy" aria-label="增加数量" @click="update(() => cartStore.updateQuantity(item.skuId, item.quantity + 1))">+</button>
            </view>
          </template>
          <button v-else class="entry-button remove-button" :disabled="busy" @click="update(() => cartStore.removeItem(item.skuId))">移除</button>
        </view>
      </view>
    </view>
    <view v-else class="empty">
      <text>购物车还没有商品</text>
      <button class="entry-button" @click="goHome">去逛逛</button>
    </view>

    <view v-if="auth.isLogin && !loading && !error && cartStore.items.length" class="bottom-bar">
      <button class="check-all" :disabled="busy || !hasPurchasable" @click="update(cartStore.toggleAll)">{{ allChecked ? '取消全选' : '全选' }}</button>
      <text class="total">合计 ¥{{ cartStore.checkedAmount.toFixed(2) }}</text>
      <button class="checkout-button" @click="checkout" :disabled="busy || !cartStore.checkedItems.length">去结算</button>
    </view>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { useAuthStore } from '@/stores/auth'
import { isPurchasable, useCartStore } from '@/stores/cart'
import ProductImage from '@/components/ProductImage.vue'

const auth = useAuthStore()
const cartStore = useCartStore()
const loading = ref(false)
const busy = ref(false)
const error = ref('')
const hasPurchasable = computed(() => cartStore.items.some(isPurchasable))
const allChecked = computed(() => hasPurchasable.value && cartStore.items.filter(isPurchasable).every(item => item.checked))
onShow(loadCart)

async function loadCart() {
  error.value = ''
  if (!auth.isLogin) return
  loading.value = true
  try {
    const res = await cartStore.fetchCart()
    if (res.code !== 200 && auth.isLogin) error.value = res.message || '购物车加载失败'
  } finally {
    loading.value = false
  }
}

async function update(operation) {
  if (busy.value) return
  busy.value = true
  try {
    const res = await operation()
    if (res.code !== 200) uni.showToast({ title: res.message || '操作失败，请重试', icon: 'none' })
  } finally {
    busy.value = false
  }
}

function checkout() {
  if (cartStore.checkedItems.length) uni.navigateTo({ url: '/pages/checkout/index' })
}
function goLogin() { uni.navigateTo({ url: '/pages/login/index' }) }
function goHome() { uni.switchTab({ url: '/pages/index/index' }) }
</script>

<style scoped>
.page { padding-bottom: 146rpx; }
.cart-list { padding: 24rpx; }
.cart-item { display: flex; align-items: center; gap: 14rpx; background: #fff; border-radius: 20rpx; padding: 20rpx 16rpx; margin-bottom: 16rpx; }
.checkbox { flex: 0 0 66rpx; width: 66rpx; height: 66rpx; line-height: 66rpx; padding: 0; margin: 0; border-radius: 50%; font-size: 31rpx; color: #64748B; background: #F1F5F9; }
.checkbox.checked { background: #B45309; color: #fff; }
.item-info { flex: 1; min-width: 0; }
.item-name { font-size: 28rpx; display: block; margin-bottom: 8rpx; overflow-wrap: anywhere; }
.qty-row { display: flex; align-items: center; gap: 12rpx; margin-top: 8rpx; }
.qty-btn { width: 82rpx; height: 82rpx; line-height: 82rpx; padding: 0; margin: 0; background: #F1F3F5; border-radius: 18rpx; font-size: 36rpx; color: #374151; }
.empty { text-align: center; padding: 120rpx 24rpx; color: #5F6B7A; }
.entry-button { margin-top: 20rpx; font-size: 27rpx; background: #FDEBC7; color: #78350F; }
.remove-button { margin-left: 0; }
.bottom-bar { position: fixed; bottom: var(--window-bottom, 0px); left: 0; right: 0; display: flex; align-items: center; padding: 16rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; gap: 10rpx; z-index: 2; }
.check-all { margin: 0; padding: 0 12rpx; font-size: 23rpx; color: #374151; background: #F1F3F5; }
.total { flex: 1; font-size: 25rpx; font-weight: bold; }
.checkout-button { margin: 0; padding: 0 22rpx; border-radius: 18rpx; height: 88rpx; line-height: 88rpx; background: #B45309; color: #fff; font-size: 28rpx; }
.checkout-button[disabled] { background: #E5E7EB; color: #5F6B7A; }
.check-all::after, .checkout-button::after, .checkbox::after, .qty-btn::after { border: 0; }
</style>
