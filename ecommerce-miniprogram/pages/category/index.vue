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
          <ProductImage :source="p.mainImage" height="240rpx" />
          <text class="product-name">{{ p.name }}</text>
          <text class="text-price">¥{{ p.minPrice ?? p.price ?? "—" }}</text>
        </view>
      </view>
      <view v-if="loading" class="empty">加载中…</view>
      <view v-else-if="error" class="empty">
        <text>{{ error }}</text>
        <button class="retry" @click="retry">重试</button>
      </view>
      <view v-else-if="!products.length" class="empty">暂无商品</view>
      <view v-if="loadingMore" class="load-tip">加载中...</view>
      <view v-else-if="noMore && products.length > 0" class="load-tip">没有更多了</view>
    </scroll-view>
  </view>
</template>

<script setup>
import { ref, computed } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { request } from '@/utils/api'
import ProductImage from '@/components/ProductImage.vue'

const categories = ref([])
const products = ref([])
const activeId = ref(null)
const page = ref(1)
const size = 20
const total = ref(0)
const loading = ref(false)
const loadingMore = ref(false)
const error = ref('')
const noMore = computed(() => products.value.length >= total.value)
let requestVersion = 0

onShow(loadCategories)

async function loadCategories() {
  const selectedId = uni.getStorageSync('browseCategoryId')
  if (!categories.value.length) {
    loading.value = true
    error.value = ''
    const res = await request({ url: '/api/v1/categories' })
    loading.value = false
    if (res.code !== 200) {
      error.value = res.message || '分类加载失败'
      return
    }
    categories.value = res.data || []
  }
  if (selectedId) uni.removeStorageSync('browseCategoryId')
  const selected = categories.value.find(item => String(item.id) === String(selectedId))
  const id = selected?.id ?? activeId.value ?? categories.value[0]?.id
  if (id != null && (id !== activeId.value || error.value)) await selectCat(id)
}

async function selectCat(id) {
  const version = ++requestVersion
  activeId.value = id
  page.value = 0
  products.value = []
  total.value = 0
  error.value = ''
  loading.value = true
  loadingMore.value = false
  await loadPage(1, version)
  if (version === requestVersion) loading.value = false
}

async function loadPage(nextPage, version) {
  const res = await request({ url: `/api/v1/products?categoryId=${encodeURIComponent(activeId.value)}&page=${nextPage}&size=${size}` })
  if (version !== requestVersion) return
  if (res.code !== 200) {
    error.value = res.message || '商品加载失败'
    return
  }
  const records = res.data?.records || []
  products.value.push(...records)
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

function retry() {
  if (activeId.value == null) loadCategories()
  else if (!page.value) selectCat(activeId.value)
  else {
    error.value = ''
    loadMore()
  }
}

const goDetail = id => uni.navigateTo({ url: `/pages/product/detail?id=${encodeURIComponent(id)}` })
</script>

<style scoped>
.page { display: flex; height: calc(100vh - var(--window-top, 0px) - var(--window-bottom, 0px)); }
.left { width: 180rpx; background: #fff; }
.cat-item { padding: 32rpx 24rpx; font-size: 26rpx; color: #6B7280; text-align: center; border-left: 4rpx solid transparent; }
.cat-item.active { color: #F59E0B; font-weight: 600; background: #FEF3C7; border-left-color: #F59E0B; }
.right { flex: 1; min-width: 0; box-sizing: border-box; padding: 24rpx; }
.product-grid { display: flex; flex-wrap: wrap; gap: 16rpx; }
.product-card { width: calc(50% - 8rpx); background: #fff; border-radius: 20rpx; overflow: hidden; padding-bottom: 12rpx; }
.product-name { font-size: 26rpx; padding: 8rpx 12rpx; display: -webkit-box; -webkit-box-orient: vertical; -webkit-line-clamp: 2; overflow: hidden; }
.product-card .text-price { padding: 0 12rpx; }
.empty { text-align: center; padding: 100rpx 0; color: #9CA3AF; }
.total-info { font-size: 24rpx; color: #9CA3AF; padding: 8rpx 0 16rpx; }
.retry { margin-top: 16rpx; font-size: 26rpx; color: #78350F; background: #FDEBC7; }
.load-tip { text-align: center; padding: 24rpx 0; color: #9CA3AF; font-size: 24rpx; }
</style>
