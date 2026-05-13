<template>
  <view class="page">
    <!-- Search bar -->
    <view class="search-bar" @click="goSearch">
      <view class="search-input">搜索商品...</view>
    </view>

    <!-- Banner -->
    <swiper class="banner" indicator-dots autoplay circular interval="3000">
      <swiper-item v-for="b in banners" :key="b">
        <image :src="b" mode="aspectFill" class="banner-img" />
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

    <!-- Hot products -->
    <view class="section">
      <view class="section-header">
        <text class="section-title">热门商品</text>
      </view>
      <view class="product-grid">
        <view v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <image :src="getImageUrl(p.mainImage)" mode="aspectFill" class="product-img" />
          <view class="product-info">
            <text class="product-name">{{ p.name }}</text>
            <view class="product-price-row">
              <text class="text-price">¥{{ p.minPrice || p.price }}</text>
              <text class="text-price-original" v-if="p.maxPrice && p.maxPrice !== p.minPrice">¥{{ p.maxPrice }}</text>
            </view>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'
import { getImageUrl } from '@/utils/image'

const banners = ref(['/static/banner_hero.png', '/static/banner_new.png', '/static/banner_seckill.png'])
const categories = ref([])
const products = ref([])
const seckillItems = ref([])

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
const goCategory = (id) => uni.navigateTo({ url: `/pages/category/index?id=${id}` })
const goSeckill = () => uni.navigateTo({ url: '/pages/seckill/index' })
</script>

<style scoped>
.page { padding-bottom: 20rpx; }
.search-bar { padding: 16rpx 24rpx; background: #fff; }
.search-input { height: 68rpx; background: #F8F8F8; border-radius: 34rpx; padding: 0 32rpx; display: flex; align-items: center; color: #9CA3AF; font-size: 28rpx; }
.banner { height: 320rpx; margin: 16rpx 24rpx; border-radius: 20rpx; overflow: hidden; }
.banner-img { width: 100%; height: 100%; }
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
.product-img { width: 100%; height: 340rpx; background: #F8F8F8; }
.product-info { padding: 16rpx; }
.product-name { font-size: 28rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.product-price-row { display: flex; align-items: baseline; gap: 8rpx; margin-top: 8rpx; }
</style>
