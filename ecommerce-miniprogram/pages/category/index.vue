<template>
  <view class="page">
    <scroll-view scroll-y class="left">
      <view v-for="c in categories" :key="c.id" :class="['cat-item', activeId === c.id ? 'active' : '']" @click="selectCat(c.id)">
        {{ c.name }}
      </view>
    </scroll-view>
    <scroll-view scroll-y class="right" @scrolltolower="loadMore">
      <view v-if="total > 0" class="total-info">共 {{ total }} 件商品</view>
      <view class="product-grid">
        <view v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <image :src="getImageUrl(p.mainImage)" mode="aspectFill" class="product-img" />
          <text class="product-name">{{ p.name }}</text>
          <text class="text-price">¥{{ p.minPrice || p.price }}</text>
        </view>
      </view>
      <view v-if="!products.length" class="empty">暂无商品</view>
      <view v-if="loadingMore" class="load-tip">加载中...</view>
      <view v-else-if="noMore && products.length > 0" class="load-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { request } from '@/utils/api'
import { getImageUrl } from '@/utils/image'

const categories = ref([])
const products = ref([])
const activeId = ref(null)
const page = ref(1)
const size = 20
const total = ref(0)
const loadingMore = ref(false)
const noMore = computed(() => products.value.length >= total.value)

onMounted(async () => {
  const res = await request({ url: '/api/v1/categories' })
  if (res.code === 200 && res.data?.length) {
    categories.value = res.data
    selectCat(res.data[0].id)
  }
})

async function selectCat(id) {
  activeId.value = id
  page.value = 1
  products.value = []
  total.value = 0
  loadingMore.value = false
  const res = await request({ url: `/api/v1/products?categoryId=${id}&page=${page.value}&size=${size}` })
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
    const res = await request({ url: `/api/v1/products?categoryId=${activeId.value}&page=${page.value}&size=${size}` })
    if (res.code === 200) {
      products.value.push(...(res.data?.records || []))
      total.value = res.data?.total || total.value
    }
  } finally { loadingMore.value = false }
}

const goDetail = (id) => uni.navigateTo({ url: `/pages/product/detail?id=${id}` })
</script>

<style scoped>
.page { display: flex; height: 100vh; }
.left { width: 180rpx; background: #fff; }
.cat-item { padding: 32rpx 24rpx; font-size: 26rpx; color: #6B7280; text-align: center; border-left: 4rpx solid transparent; }
.cat-item.active { color: #F59E0B; font-weight: 600; background: #FEF3C7; border-left-color: #F59E0B; }
.right { flex: 1; padding: 24rpx; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.product-card { width: calc(50% - 8rpx); background: #fff; border-radius: 20rpx; overflow: hidden; padding-bottom: 12rpx; }
.product-img { width: 100%; height: 280rpx; background: #F8F8F8; }
.product-name { font-size: 26rpx; padding: 8rpx 12rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.product-card .text-price { padding: 0 12rpx; }
.empty { text-align: center; padding: 100rpx 0; color: #9CA3AF; }
.total-info { font-size: 24rpx; color: #9CA3AF; padding: 8rpx 0 16rpx; }
.load-tip { text-align: center; padding: 24rpx 0; color: #9CA3AF; font-size: 24rpx; }
</style>
