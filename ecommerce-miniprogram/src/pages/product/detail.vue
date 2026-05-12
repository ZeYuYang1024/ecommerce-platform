<template>
  <view class="page" v-if="product">
    <!-- Image swiper -->
    <swiper class="swiper" indicator-dots>
      <swiper-item v-for="img in images" :key="img">
        <image :src="img" mode="aspectFill" class="swiper-img" />
      </swiper-item>
    </swiper>

    <!-- Info -->
    <view class="card">
      <text class="price">¥{{ product.minPrice || product.price }}</text>
      <text class="price-original" v-if="product.maxPrice && product.maxPrice > product.minPrice">¥{{ product.maxPrice }}</text>
      <text class="name">{{ product.name }}</text>
      <text class="desc">{{ product.description }}</text>
      <view class="meta">
        <text>评分 {{ product.avgRating || '-' }}</text>
        <text>{{ product.reviewCount || 0 }} 评价</text>
      </view>
    </view>

    <!-- Bottom bar -->
    <view class="bottom-bar">
      <view class="cart-btn" @click="goCart">
        <text>购物车</text>
      </view>
      <button class="btn-primary" @click="addCart">加入购物车</button>
      <button class="btn-buy" @click="buyNow">立即购买</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'
import { useCartStore } from '@/stores/cart'

const cartStore = useCartStore()
const product = ref(null)
const images = ref([])

onMounted(async () => {
  const { id } = __uniGetLaunchOptionsSync ? {} : {} || {}
  // For uni-app, use onLoad query
})

// In uni-app, we use onLoad
import { onLoad } from '@dcloudio/uni-app'
onLoad(async (options) => {
  const id = options?.id
  if (!id) return
  const res = await request({ url: `/api/v1/products/${id}` })
  if (res.code === 200 && res.data?.spu) {
    product.value = res.data.spu
    const imgs = [res.data.spu.mainImage].filter(Boolean)
    if (res.data.spu.images) imgs.push(...res.data.spu.images.split(','))
    if (!imgs.length) imgs.push('/static/product_01.png')
    images.value = imgs
  }
})

async function addCart() {
  if (product.value?.skus?.[0]) {
    await cartStore.addToCart(product.value.skus[0].id)
    uni.showToast({ title: '已加入购物车', icon: 'success' })
  }
}

function buyNow() { uni.navigateTo({ url: '/pages/checkout/index' }) }
function goCart() { uni.switchTab({ url: '/pages/cart/index' }) }
</script>

<style scoped>
.swiper { height: 600rpx; }
.swiper-img { width: 100%; height: 100%; background: #F8F8F8; }
.card { padding: 24rpx; background: #fff; margin: -30rpx 0 0; border-radius: 30rpx 30rpx 0 0; position: relative; z-index: 1; }
.price { font-size: 48rpx; color: #EF4444; font-weight: bold; }
.price-original { font-size: 28rpx; color: #9CA3AF; text-decoration: line-through; margin-left: 12rpx; }
.name { display: block; font-size: 34rpx; font-weight: 600; margin: 16rpx 0 8rpx; }
.desc { font-size: 26rpx; color: #9CA3AF; display: block; margin-bottom: 16rpx; }
.meta { display: flex; gap: 24rpx; font-size: 24rpx; color: #6B7280; }
.bottom-bar { position: fixed; bottom: 0; left: 0; right: 0; display: flex; align-items: center; padding: 16rpx 24rpx; padding-bottom: calc(16rpx + env(safe-area-inset-bottom)); background: #fff; border-top: 1px solid #E5E7EB; gap: 16rpx; }
.cart-btn { font-size: 24rpx; color: #6B7280; }
.btn-buy { flex: 1; background: linear-gradient(135deg, #F59E0B, #F97316); color: #fff; border-radius: 40rpx; font-size: 28rpx; font-weight: 500; border: none; height: 80rpx; line-height: 80rpx; }
</style>
