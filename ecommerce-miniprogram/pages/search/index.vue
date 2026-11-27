<template>
  <view class="page">
    <view class="search-bar">
      <input v-model="keyword" placeholder="搜索商品..." class="search-input" @confirm="doSearch" focus />
      <text class="search-btn" @click="doSearch">搜索</text>
    </view>
    <view v-if="loading" class="empty">搜索中…</view>
    <view v-else-if="error && !products.length" class="empty">
      <text>{{ error }}</text>
      <button class="retry" @click="doSearch">重试</button>
    </view>
    <scroll-view v-else-if="submittedKeyword" scroll-y class="result-list" @scrolltolower="loadMore">
      <view v-if="total > 0" class="total-info">共 {{ total }} 件商品</view>
      <view class="product-grid" v-if="products.length">
        <view v-for="p in products" :key="p.id" class="product-card" @click="goDetail(p.id)">
          <ProductImage :source="p.mainImage" height="340rpx" />
          <view class="product-info">
            <text class="product-name">{{ p.name }}</text>
            <text class="text-price">¥{{ p.minPrice }}</text>
          </view>
        </view>
      </view>
      <view v-else-if="total === 0" class="empty">未找到相关商品</view>
      <view v-if="loadingMore" class="load-tip">加载中…</view>
      <view v-else-if="error" class="load-tip">
        <text>{{ error }}</text>
        <button class="retry" @click="retryMore">重试</button>
      </view>
      <view v-else-if="noMore && products.length > 0" class="load-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { request } from '@/utils/api'
import ProductImage from '@/components/ProductImage.vue'

const keyword = ref('')
const submittedKeyword = ref('')
const products = ref([])
const total = ref(0)
const page = ref(0)
const size = 20
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const noMore = computed(() => products.value.length >= total.value)
let requestVersion = 0

async function doSearch() {
  const query = keyword.value.trim()
  if (!query) {
    uni.showToast({ title: '请输入搜索关键词', icon: 'none' })
    return
  }
  const version = ++requestVersion
  submittedKeyword.value = query
  page.value = 0
  products.value = []
  total.value = 0
  error.value = ''
  loadingMore.value = false
  loading.value = true
  await loadPage(1, version)
  if (version === requestVersion) loading.value = false
}

async function loadPage(nextPage, version) {
  const res = await request({ url: `/api/v1/search?keyword=${encodeURIComponent(submittedKeyword.value)}&page=${nextPage}&size=${size}` })
  if (version !== requestVersion) return
  if (res.code !== 200) {
    error.value = res.message || '搜索失败，请重试'
    return
  }
  products.value.push(...(res.data?.records || []))
  total.value = Number(res.data?.total ?? products.value.length)
  page.value = nextPage
}

async function loadMore() {
  if (loading.value || loadingMore.value || noMore.value || error.value) return
  const version = requestVersion
  loadingMore.value = true
  await loadPage(page.value + 1, version)
  if (version === requestVersion) loadingMore.value = false
}

function retryMore() {
  error.value = ''
  loadMore()
}

const goDetail = id => uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(id)}` })
</script>

<style scoped>
.page { height: calc(100vh - var(--window-top, 0px)); padding: 24rpx; box-sizing: border-box; display: flex; flex-direction: column; }
.result-list { flex: 1; min-height: 0; }
.search-bar { display: flex; gap: 16rpx; align-items: center; margin-bottom: 24rpx; }
.search-input { flex: 1; min-width: 0; height: 72rpx; background: #fff; border-radius: 36rpx; padding: 0 32rpx; font-size: 28rpx; }
.search-btn { font-size: 28rpx; color: #fff; background: #F59E0B; padding: 14rpx 32rpx; border-radius: 36rpx; font-weight: 500; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.product-card { width: calc(50% - 8rpx); background: #fff; border-radius: 20rpx; overflow: hidden; }
.product-info { padding: 16rpx; }
.product-name { font-size: 28rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.empty { text-align: center; padding: 100rpx 0; color: #9CA3AF; font-size: 28rpx; }
.retry { margin-top: 16rpx; font-size: 26rpx; color: #78350F; background: #FDEBC7; }
.total-info { font-size: 24rpx; color: #9CA3AF; padding: 8rpx 0 16rpx; }
.load-tip { text-align: center; padding: 24rpx 0; color: #9CA3AF; font-size: 24rpx; }
</style>
