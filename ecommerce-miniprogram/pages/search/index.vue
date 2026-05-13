<template>
  <view class="page">
    <view class="search-bar">
      <input v-model="keyword" placeholder="搜索商品..." class="search-input" @confirm="doSearch" focus />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>
    <scroll-view scroll-y v-if="total >= 0" class="result-list" @scrolltolower="loadMore">
      <view v-if="total > 0" class="total-info">共 {{ total }} 件商品</view>
      <view class="product-grid" v-if="products.length">
        <view v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <image :src="getImageUrl(p.mainImage)" mode="aspectFill" class="product-img" />
          <view class="product-info">
            <text class="product-name">{{ p.name }}</text>
            <text class="text-price">¥{{ p.minPrice }}</text>
          </view>
        </view>
      </view>
      <view v-else-if="total === 0" class="empty">未找到相关商品</view>
      <view v-if="loadingMore" class="load-tip">加载中...</view>
      <view v-else-if="noMore && products.length > 0" class="load-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { request } from '@/utils/api'
import { getImageUrl } from '@/utils/image'

const keyword = ref('')
const products = ref([])
const total = ref(-1)
const page = ref(1)
const size = 20
const loadingMore = ref(false)
const noMore = computed(() => products.value.length >= total.value)

async function doSearch() {
  page.value = 1
  products.value = []
  total.value = -1
  loadingMore.value = false
  const res = await request({ url: `/api/v1/search?keyword=${encodeURIComponent(keyword.value)}&page=${page.value}&size=${size}` })
  if (res.code === 200) {
    products.value = res.data?.records || []
    total.value = res.data?.total || 0
  }
}

async function loadMore() {
  if (loadingMore.value || noMore.value) return
  loadingMore.value = true
  page.value++
  try {
    const res = await request({ url: `/api/v1/search?keyword=${encodeURIComponent(keyword.value)}&page=${page.value}&size=${size}` })
    if (res.code === 200) {
      products.value.push(...(res.data?.records || []))
      total.value = res.data?.total || total.value
    }
  } finally { loadingMore.value = false }
}

const goDetail = (id) => uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
</script>

<style scoped>
.page { padding: 24rpx; }
.search-bar { display: flex; gap: 16rpx; align-items: center; margin-bottom: 24rpx; }
.search-input { flex: 1; height: 72rpx; background: #fff; border-radius: 36rpx; padding: 0 32rpx; font-size: 28rpx; }
.search-btn { font-size: 28rpx; color: #fff; background: #F59E0B; padding: 14rpx 32rpx; border-radius: 36rpx; font-weight: 500; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.product-card { width: calc(50% - 8rpx); background: #fff; border-radius: 20rpx; overflow: hidden; }
.product-img { width: 100%; height: 340rpx; background: #F8F8F8; }
.product-info { padding: 16rpx; }
.product-name { font-size: 28rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.empty { text-align: center; padding: 100rpx 0; color: #9CA3AF; font-size: 28rpx; }
.total-info { font-size: 24rpx; color: #9CA3AF; padding: 8rpx 0 16rpx; }
.load-tip { text-align: center; padding: 24rpx 0; color: #9CA3AF; font-size: 24rpx; }
</style>
