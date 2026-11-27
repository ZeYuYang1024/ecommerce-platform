<template>
  <view class="page">
    <view v-if="loading" class="state">加载中…</view>
    <view v-else-if="error" class="state">
      <text>{{ error }}</text>
      <button class="retry" @click="loadProduct">重试</button>
    </view>
    <template v-else-if="product">
      <swiper class="swiper" :indicator-dots="images.length > 1">
        <swiper-item v-for="(source, index) in images" :key="source || index">
          <ProductImage :source="source" />
        </swiper-item>
      </swiper>

      <view class="detail-card">
        <text class="price">¥{{ displayPrice }}</text>
        <text class="name">{{ product.name }}</text>
        <text v-if="product.description" class="desc">{{ product.description }}</text>
        <text class="meta">{{ product.avgRating ? '评分 ' + product.avgRating + ' · ' : '' }}{{ product.reviewCount || 0 }} 条评价</text>
      </view>

      <view v-if="skus.length" class="detail-card specs">
        <text class="spec-title">商品规格</text>
        <view class="spec-options">
          <button
            v-for="sku in skus" :key="sku.id"
            :class="['sku-option', selectedSkuId === sku.id ? 'selected' : '']"
            @click="selectedSkuId = sku.id"
          >{{ sku.name }}</button>
        </view>
      </view>

      <view class="bottom-bar">
        <button class="cart-link" @click="goCart">购物车</button>
        <button class="add-button" :disabled="!canPurchase || adding" @click="addCart">加入购物车</button>
        <button class="buy-button" :disabled="!canPurchase || adding" @click="buyNow">立即购买</button>
      </view>
    </template>
  </view>
</template>

<script setup>
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import { productImages } from '@/utils/image'
import { useAuthStore } from '@/stores/auth'
import { useCartStore } from '@/stores/cart'
import ProductImage from '@/components/ProductImage.vue'

const auth = useAuthStore()
const cartStore = useCartStore()
const product = ref(null)
const skus = ref([])
const selectedSkuId = ref(null)
const selectedSku = computed(() => skus.value.find(sku => sku.id === selectedSkuId.value))
const images = computed(() => {
  const gallery = product.value ? productImages(product.value) : []
  return gallery.length ? gallery : ['']
})
const displayPrice = computed(() => {
  const price = selectedSku.value?.price ?? product.value?.minPrice
  return price == null ? '—' : Number(price).toFixed(2)
})
const canPurchase = computed(() => Number(product.value?.status) === 1 && skus.value.length > 0)
const loading = ref(true)
const adding = ref(false)
const error = ref('')
let productId

onLoad(options => {
  productId = options?.id
  loadProduct()
})

async function loadProduct() {
  if (!productId) {
    loading.value = false
    error.value = '商品不存在'
    return
  }
  loading.value = true
  error.value = ''
  const res = await request({ url: `/api/v1/products/${encodeURIComponent(productId)}` })
  if (res.code === 200 && res.data?.spu) {
    product.value = res.data.spu
    skus.value = (res.data.skus || []).filter(sku => sku.id != null && sku.price != null)
    selectedSkuId.value = skus.value.length === 1 ? skus.value[0].id : null
  } else {
    error.value = res.message || '商品加载失败'
  }
  loading.value = false
}

function requireSelection() {
  if (!auth.isLogin) {
    uni.navigateTo({ url: '/pages/login/index' })
    return false
  }
  if (!selectedSku.value) {
    uni.showToast({ title: '请先选择规格', icon: 'none' })
    return false
  }
  return true
}

async function addCart() {
  if (adding.value || !canPurchase.value || !requireSelection()) return
  adding.value = true
  try {
    const sku = selectedSku.value
    const spu = product.value
    const res = await cartStore.addToCart({
      skuId: sku.id, spuId: spu.id, name: sku.name, image: sku.image || spu.mainImage,
      price: sku.price, quantity: 1
    })
    if (res.code === 401) uni.navigateTo({ url: '/pages/login/index' })
    else uni.showToast({ title: res.code === 200 ? '已加入购物车' : res.message || '加购失败', icon: res.code === 200 ? 'success' : 'none' })
  } finally {
    adding.value = false
  }
}

function buyNow() {
  if (!canPurchase.value || !requireSelection()) return
  uni.navigateTo({
    url: `/pages/checkout/index?spuId=${encodeURIComponent(product.value.id)}&skuId=${encodeURIComponent(selectedSkuId.value)}`
  })
}

function goCart() { uni.switchTab({ url: '/pages/cart/index' }) }
</script>

<style scoped>
.page { padding-bottom: 132rpx; }
.swiper { height: 600rpx; }
.detail-card { padding: 28rpx; background: #fff; }
.specs { margin-top: 16rpx; }
.price { font-size: 48rpx; color: #B91C1C; font-weight: bold; }
.name { display: block; font-size: 34rpx; font-weight: 600; margin: 16rpx 0 8rpx; }
.desc, .meta { display: block; font-size: 26rpx; color: #5F6B7A; line-height: 1.55; }
.meta { margin-top: 8rpx; }
.spec-title { display: block; font-weight: 600; margin-bottom: 16rpx; }
.spec-options { display: flex; flex-wrap: wrap; gap: 12rpx; }
.sku-option { margin: 0; padding: 10rpx 20rpx; line-height: 1.6; min-height: 76rpx; display: flex; align-items: center; font-size: 25rpx; border: 2rpx solid #E2E8F0; border-radius: 14rpx; background: #fff; color: #1F2937; }
.sku-option.selected { border-color: #B45309; background: #FFF4DF; color: #92400E; }
.sku-option::after, .bottom-bar button::after { border: 0; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; gap: 12rpx; z-index: 2; }
.bottom-bar button { margin: 0; height: 88rpx; line-height: 88rpx; padding: 0 12rpx; font-size: 26rpx; border-radius: 20rpx; }
.cart-link { background: #F1F3F5; color: #374151; }
.add-button { flex: 1; background: #FDEBC7; color: #78350F; }
.buy-button { flex: 1; background: #B45309; color: #fff; }
.bottom-bar button[disabled] { color: #5F6B7A; background: #E5E7EB; }
.state { text-align: center; padding: 96rpx 32rpx; color: #5F6B7A; }
.retry { margin-top: 24rpx; }
</style>
