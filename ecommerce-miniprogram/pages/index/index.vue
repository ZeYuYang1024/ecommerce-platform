<template>
  <view class="page">
    <!-- Search bar -->
    <view class="search-bar" @click="goSearch">
      <view class="search-input">搜索商品...</view>
    </view>

    <!-- Banner -->
    <swiper class="banner" indicator-dots autoplay circular interval="3000">
      <swiper-item v-for="banner in banners" :key="banner.title">
        <view :class="['banner-panel', banner.theme]">
          <text class="banner-title">{{ banner.title }}</text>
          <text class="banner-subtitle">{{ banner.subtitle }}</text>
        </view>
      </swiper-item>
    </swiper>

    <!-- Category icons -->
    <view class="cats">
      <view v-for="c in categories" :key="c.id" class="cat-item" @click="goCategory(c.id)">
        <view class="cat-icon">{{ c.name ? c.name.charAt(0) : '?' }}</view>
        <text class="cat-text">{{ c.name }}</text>
      </view>
    </view>

    <!-- Seckill -->
    <view v-if="seckillItems.length" class="section">
      <view class="section-header" @click="goSeckill">
        <text class="section-title">限时秒杀</text>
        <text class="section-more">更多 →</text>
      </view>
      <scroll-view scroll-x class="h-scroll">
        <view v-for="item in seckillItems" :key="item.id" class="seckill-card" @click="goDetail(item.spuId)">
          <text class="seckill-price">¥{{ item.seckillPrice }}</text>
          <text class="seckill-name">{{ item.name }}</text>
        </view>
      </scroll-view>
    </view>

    <view v-if="feed?.items.length" class="section">
      <view class="section-header">
        <text class="section-title">猜你喜欢</text>
      </view>
      <view class="product-grid">
        <view v-for="p in feed.items" :id="'recommendation-' + p.id" :key="p.id"
              class="product-card" @click="openRecommendation(p.id)">
          <ProductImage :source="p.mainImage" height="340rpx" />
          <view class="product-info">
            <text class="product-name">{{ p.name }}</text>
            <view class="product-price-row">
              <text class="text-price">¥{{ p.minPrice ?? "—" }}</text>
              <text class="price-range" v-if="Number(p.maxPrice) > Number(p.minPrice)">– ¥{{ p.maxPrice }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>

    <!-- Hot products -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">热门商品</text>
      </view>
      <view class="product-grid">
        <view v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <ProductImage :source="p.mainImage" height="340rpx" />
          <view class="product-info">
            <text class="product-name">{{ p.name }}</text>
            <view class="product-price-row">
              <text class="text-price">¥{{ p.minPrice ?? p.price ?? "—" }}</text>
              <text class="price-range" v-if="Number(p.maxPrice) > Number(p.minPrice)">– ¥{{ p.maxPrice }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { getCurrentInstance, nextTick, onMounted, ref, watch } from 'vue'
import { onHide, onShow, onUnload } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import { loadRecommendations, recordRecommendationEvents } from '@/utils/recommendation'
import { token } from '@/utils/session'
import ProductImage from '@/components/ProductImage.vue'

const banners = [
  { title: '品质好物，一站购齐', subtitle: '从日常所需到心仪新品', theme: 'amber' },
  { title: '发现新上架好物', subtitle: '看看最近上架的商品', theme: 'teal' },
  { title: '发现购物优惠', subtitle: '逛逛优惠活动和领券中心', theme: 'plum' }
]
const categories = ref([])
const products = ref([])
const seckillItems = ref([])
const feed = ref(null)
const pageContext = getCurrentInstance()?.proxy
const impressions = new Set()
const observers = []
let visible = false
let loadVersion = 0

function stopObserving() {
  observers.splice(0).forEach(observer => observer.disconnect())
}

async function refreshRecommendations() {
  const version = ++loadVersion
  stopObserving()
  feed.value = null
  impressions.clear()
  const result = await loadRecommendations()
  if (!visible || version !== loadVersion || !result?.items?.length) return
  feed.value = result
  await nextTick()
  if (!visible || version !== loadVersion || !result.requestId) return

  for (const product of result.items) {
    const observer = uni.createIntersectionObserver(pageContext, { thresholds: [0.5] })
    observers.push(observer)
    observer.relativeToViewport().observe(`#recommendation-${product.id}`, event => {
      if (!visible || version !== loadVersion || event.intersectionRatio < 0.5 || impressions.has(product.id)) return
      impressions.add(product.id)
      observer.disconnect()
      recordRecommendationEvents([{ type: 'IMPRESSION', spuId: product.id, requestId: result.requestId }])
    })
  }
}

onShow(() => {
  visible = true
  void refreshRecommendations()
})
function hideRecommendations() {
  visible = false
  ++loadVersion
  stopObserving()
}
onHide(hideRecommendations)
onUnload(hideRecommendations)
watch(token, () => {
  if (visible) void refreshRecommendations()
})

function openRecommendation(spuId) {
  const requestId = feed.value?.requestId
  if (requestId) {
    impressions.add(spuId)
    // A click confirms visibility even when the observer request is still in flight.
    recordRecommendationEvents([{ type: 'IMPRESSION', spuId, requestId }, { type: 'CLICK', spuId, requestId }])
  }
  goDetail(spuId)
}

onMounted(async () => {
  try {
    const [catRes, prodRes, secRes] = await Promise.all([
      request({ url: '/api/v1/categories' }),
      request({ url: '/api/v1/products?page=1&size=10' }),
      request({ url: '/api/v1/seckill/sessions' })
    ])
    if (catRes.code === 200) categories.value = (catRes.data || []).slice(0, 8)
    if (prodRes.code === 200) products.value = prodRes.data?.records || []
    if (secRes.code === 200 && secRes.data?.length) {
      const sid = secRes.data[0].id
      const iRes = await request({ url: `/api/v1/seckill/items?sessionId=${sid}` })
      if (iRes.code === 200) seckillItems.value = (iRes.data || []).slice(0, 5)
    }
  } catch {}
})

const goSearch = () => uni.navigateTo({ url: '/pages/search/index' })
const goDetail = (id) => uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
function goCategory(id) {
  uni.setStorageSync('browseCategoryId', String(id))
  uni.switchTab({ url: '/pages/category/index', fail: () => uni.removeStorageSync('browseCategoryId') })
}
const goSeckill = () => uni.navigateTo({ url: '/pages/seckill/index' })
</script>

<style scoped>
.page { padding-bottom: 20rpx; }
.search-bar { padding: 16rpx 24rpx; background: #fff; }
.search-input { height: 68rpx; background: #F8F8F8; border-radius: 34rpx; padding: 0 32rpx; display: flex; align-items: center; color: #9CA3AF; font-size: 28rpx; }
.banner { height: 320rpx; margin: 16rpx 24rpx; border-radius: 20rpx; overflow: hidden; }
.banner-panel { height: 100%; box-sizing: border-box; padding: 48rpx; display: flex; flex-direction: column; justify-content: center; gap: 12rpx; color: #fff; background: radial-gradient(circle at 100% 0%, #B45309, transparent 58%), #7C2D12; }
.banner-panel.teal { background: radial-gradient(circle at 100% 0%, #0F766E, transparent 58%), #134E4A; }
.banner-panel.plum { background: radial-gradient(circle at 100% 0%, #A21CAF, transparent 58%), #581C87; }
.banner-title { font-size: 48rpx; font-weight: 700; }
.banner-subtitle { font-size: 27rpx; }
.cats { display: flex; flex-wrap: wrap; padding: 16rpx 24rpx; background: #fff; margin: 0 24rpx; border-radius: 20rpx; }
.cat-item { width: 25%; display: flex; flex-direction: column; align-items: center; padding: 16rpx 0; }
.cat-icon { width: 80rpx; height: 80rpx; background: linear-gradient(135deg, #FEF3C7, #FDE68A); border-radius: 20rpx; display: flex; align-items: center; justify-content: center; font-size: 32rpx; font-weight: bold; color: #D97706; }
.cat-text { font-size: 24rpx; color: #6B7280; margin-top: 8rpx; }
.section { margin: 24rpx; }
.section-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 16rpx; }
.section-title { font-size: 34rpx; font-weight: bold; }
.section-more { font-size: 26rpx; color: #D97706; }
.h-scroll { white-space: nowrap; }
.seckill-card { display: inline-block; width: 200rpx; background: #fff; border-radius: 16rpx; padding: 16rpx; margin-right: 16rpx; text-align: center; }
.seckill-price { color: #EF4444; font-size: 30rpx; font-weight: bold; display: block; }
.seckill-name { font-size: 24rpx; color: #6B7280; margin-top: 6rpx; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; display: block; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.product-card { width: calc(50% - 8rpx); background: #fff; border-radius: 20rpx; overflow: hidden; }
.product-info { padding: 16rpx; }
.product-name { font-size: 28rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.price-range { color: #5F6B7A; font-size: 24rpx; }
.product-price-row { display: flex; align-items: baseline; gap: 8rpx; margin-top: 8rpx; }
</style>
