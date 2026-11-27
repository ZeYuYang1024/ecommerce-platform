<template>
  <view class="page">
    <view v-if="!auth.isLogin" class="notice">
      <text>登录后继续结算</text>
      <button class="secondary" @click="goLogin">立即登录</button>
    </view>
    <view v-else-if="loading" class="notice">正在准备订单…</view>
    <view v-else-if="error" class="notice">
      <text>{{ error }}</text>
      <button class="secondary" @click="loadCheckout">重试</button>
    </view>
    <view v-else-if="!items.length" class="notice">没有可结算的商品</view>
    <template v-else>
      <view class="checkout-card">
        <view class="address-heading">
          <text class="label">收货地址</text>
          <button class="action" @click="chooseAddress">{{ address ? '更换' : '选择地址' }}</button>
        </view>
        <view v-if="address" class="address-summary" @click="chooseAddress">
          <text>{{ address.receiverName }} {{ address.receiverPhone }}</text>
          <text class="address-detail">{{ fullAddress(address) }}</text>
        </view>
        <text v-else class="hint" @click="chooseAddress">请选择收货地址</text>
        <view v-if="showAddresses" class="address-options">
          <button
            v-for="item in addresses" :key="item.id"
            :class="['address-option', selectedAddressId === item.id ? 'selected' : '']"
            @click="selectAddress(item.id)"
          >
            <text>{{ item.receiverName }} {{ item.receiverPhone }}</text>
            <text class="address-detail">{{ fullAddress(item) }}</text>
          </button>
          <button class="secondary" @click="goAddresses">新增或管理地址</button>
        </view>
      </view>

      <view class="checkout-card">
        <text class="label">商品清单</text>
        <view v-for="item in items" :key="item.skuId" class="item-row">
          <ProductImage :source="item.image" width="104rpx" height="104rpx" radius="12rpx" />
          <view class="item-info">
            <text>{{ item.name }}</text>
            <text class="item-price">¥{{ Number(item.price).toFixed(2) }} × {{ item.quantity }}</text>
          </view>
        </view>
      </view>

      <view class="checkout-card amount-row">
        <text class="label">商品金额</text>
        <text class="amount">¥{{ total.toFixed(2) }}</text>
      </view>
      <text v-if="submitError" class="submit-error" role="alert">{{ submitError }}</text>

      <view class="submit-bar">
        <text class="total">合计 ¥{{ total.toFixed(2) }}</text>
        <button class="submit-button" :disabled="submitting" @click="submitOrder">{{ submitting ? '提交中…' : '提交订单' }}</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import ProductImage from '@/components/ProductImage.vue'

const auth = useAuthStore()
const cartStore = useCartStore()
const items = ref([])
const addresses = ref([])
const selectedAddressId = ref(null)
const address = computed(() => addresses.value.find(item => item.id === selectedAddressId.value))
const total = computed(() => items.value.reduce((sum, item) => sum + Number(item.price) * item.quantity, 0))
const showAddresses = ref(false)
const loading = ref(false)
const submitting = ref(false)
const error = ref('')
const submitError = ref('')
let buyNow

onLoad(options => {
  if (options?.spuId && options?.skuId) buyNow = { spuId: options.spuId, skuId: options.skuId }
})
onShow(loadCheckout)

async function loadItems() {
  if (!buyNow) {
    const res = await cartStore.fetchCart()
    if (res.code === 200) items.value = [...cartStore.checkedItems]
    return res
  }

  const res = await request({ url: `/api/v1/products/${encodeURIComponent(buyNow.spuId)}` })
  if (res.code !== 200) return res
  const spu = res.data?.spu
  const sku = res.data?.skus?.find(item => String(item.id) === String(buyNow.skuId))
  if (Number(spu?.status) !== 1 || !sku || sku.price == null) return { code: 404, message: '商品规格已不可用' }
  items.value = [{
    skuId: sku.id, spuId: spu.id, name: sku.name, image: sku.image || spu.mainImage, price: sku.price, quantity: 1
  }]
  return res
}

async function loadCheckout() {
  if (!auth.isLogin || submitting.value || loading.value) return
  loading.value = true
  error.value = ''
  try {
    const [itemsRes, addressRes] = await Promise.all([loadItems(), request({ url: '/api/v1/users/addresses' })])
    if (itemsRes.code !== 200 || addressRes.code !== 200) {
      error.value = (itemsRes.code !== 200 ? itemsRes.message : addressRes.message) || '结算信息加载失败'
      return
    }
    addresses.value = addressRes.data || []
    const newId = uni.getStorageSync('checkoutAddressId')
    if (newId) {
      uni.removeStorageSync('checkoutAddressId')
      const savedAddress = addresses.value.find(item => String(item.id) === newId)
      if (savedAddress) selectAddress(savedAddress.id)
    }
    if (!address.value) {
      selectedAddressId.value = (addresses.value.find(item => Number(item.isDefault) === 1) || addresses.value[0])?.id ?? null
    }
  } finally {
    loading.value = false
  }
}

function fullAddress(item) {
  return [item.province, item.city, item.district, item.detail].filter(Boolean).join(' ')
}

function chooseAddress() {
  if (!addresses.value.length) goAddresses()
  else showAddresses.value = !showAddresses.value
}

function selectAddress(id) {
  selectedAddressId.value = id
  showAddresses.value = false
  submitError.value = ''
}

async function submitOrder() {
  if (submitting.value || !auth.isLogin || !items.value.length) return
  const recipient = address.value
  if (!recipient) {
    submitError.value = '请选择收货地址后再提交'
    return
  }
  submitting.value = true
  submitError.value = ''
  const purchased = [...items.value]
  try {
    const res = await request({
      url: '/api/v1/orders',
      method: 'POST',
      data: {
        receiverName: recipient.receiverName,
        receiverPhone: recipient.receiverPhone,
        receiverAddress: fullAddress(recipient),
        items: purchased.map(({ skuId, quantity }) => ({ skuId, quantity }))
      }
    })
    if (res.code !== 200 || !res.data?.orderNo) {
      submitError.value = res.code === -1 ? '提交结果尚未确认，请先到我的订单查看' : res.message || '下单失败，请重试'
      return
    }

    if (!buyNow && !await cartStore.removePurchased(purchased)) {
      uni.showToast({ title: '订单已提交，购物车更新失败', icon: 'none' })
    }
    uni.redirectTo({ url: `/pages/order-detail/index?orderNo=${encodeURIComponent(res.data.orderNo)}` })
  } finally {
    submitting.value = false
  }
}

function goAddresses() { uni.navigateTo({ url: '/pages/address/index?fromCheckout=1' }) }
function goLogin() { uni.navigateTo({ url: '/pages/login/index' }) }
</script>

<style scoped>
.page { padding: 24rpx; padding-bottom: 148rpx; }
.checkout-card { background: #fff; border-radius: 20rpx; padding: 26rpx; margin-bottom: 16rpx; }
.address-heading, .amount-row, .submit-bar { display: flex; justify-content: space-between; align-items: center; gap: 16rpx; }
.label { font-size: 30rpx; font-weight: 600; display: block; }
.action { margin: 0; padding: 0 16rpx; font-size: 26rpx; color: #92400E; background: #FFF4DF; }
.address-summary, .hint { display: block; margin-top: 16rpx; font-size: 27rpx; }
.address-detail { display: block; margin-top: 8rpx; color: #5F6B7A; font-size: 25rpx; }
.hint, .notice { color: #5F6B7A; }
.address-options { margin-top: 20rpx; }
.address-option { margin-bottom: 12rpx; border: 2rpx solid #E2E8F0; background: #fff; text-align: left; padding: 16rpx; font-size: 27rpx; line-height: 1.4; }
.address-option.selected { border-color: #B45309; background: #FFF4DF; }
.item-row { display: flex; align-items: center; gap: 16rpx; margin-top: 20rpx; }
.item-info { flex: 1; min-width: 0; overflow-wrap: anywhere; }
.item-price { display: block; font-size: 26rpx; font-weight: 600; color: #B91C1C; margin-top: 8rpx; }
.amount { color: #B91C1C; font-weight: 700; }
.notice { text-align: center; padding: 96rpx 24rpx; }
.secondary { margin-top: 18rpx; font-size: 27rpx; color: #78350F; background: #FDEBC7; }
.submit-error { display: block; padding: 16rpx 4rpx; color: #B91C1C; font-size: 27rpx; }
.submit-bar { position: fixed; bottom: 0; left: 0; right: 0; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; }
.total { font-size: 28rpx; font-weight: 700; }
.submit-button { margin: 0; height: 88rpx; line-height: 88rpx; padding: 0 36rpx; background: #B45309; color: #fff; border-radius: 20rpx; font-size: 28rpx; }
.submit-button[disabled] { background: #E5E7EB; color: #5F6B7A; }
.action::after, .address-option::after, .submit-button::after { border: 0; }
</style>
