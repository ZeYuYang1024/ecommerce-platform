<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-header">
      <el-button @click="$router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回商品列表
      </el-button>
    </div>

    <div v-if="detail" class="detail-layout">
      <!-- Left: Images -->
      <div class="detail-images">
        <div class="image-gallery">
          <img v-if="detail.spu.mainImage" :src="imageUrl(detail.spu.mainImage)" class="main-image" />
          <div v-else class="main-image empty">暂无主图</div>
          <div v-if="allImages.length > 0" class="image-thumbs">
            <img v-for="(img, i) in allImages" :key="i" :src="imageUrl(img)" class="thumb-img" />
          </div>
        </div>
      </div>

      <!-- Right: Info -->
      <div class="detail-info">
        <div class="info-header">
          <h1 class="product-title">{{ detail.spu.name }}</h1>
          <el-tag :type="detail.spu.status === 1 ? 'success' : 'info'" size="large">
            {{ detail.spu.status === 1 ? '已上架' : '已下架' }}
          </el-tag>
        </div>

        <div class="info-grid">
          <div class="info-item">
            <span class="info-label">商品 ID</span>
            <span class="info-value font-mono">{{ detail.spu.id }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">平均评分</span>
            <span class="info-value">{{ detail.spu.avgRating || '-' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">评论数</span>
            <span class="info-value">{{ detail.spu.reviewCount || 0 }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">创建时间</span>
            <span class="info-value font-mono" style="font-size:13px">{{ detail.spu.createdAt }}</span>
          </div>
        </div>

        <div class="info-desc">
          <span class="info-label">简介</span>
          <p class="desc-text">{{ detail.spu.description || '暂无简介' }}</p>
        </div>

        <div class="detail-actions">
          <el-button type="primary" size="large" @click="$router.push(`/products/${detail.spu.id}/edit`)">
            <el-icon style="margin-right:6px"><Edit /></el-icon> 编辑商品
          </el-button>
          <el-button size="large" @click="$router.back()">返回</el-button>
        </div>
      </div>
    </div>

    <!-- Detail HTML -->
    <el-card v-if="detail && detail.spu.detail" class="detail-html-card" shadow="never">
      <template #header>
        <span class="card-title">商品详情</span>
      </template>
      <div class="detail-html" v-html="detail.spu.detail"></div>
    </el-card>

    <!-- SKU List -->
    <el-card v-if="detail && detail.skus && detail.skus.length > 0" class="sku-card" shadow="never">
      <template #header>
        <span class="card-title">SKU 列表</span>
      </template>
      <el-table :data="detail.skus">
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <img v-if="row.image" :src="imageUrl(row.image)" class="sku-thumb" />
            <span v-else class="no-image">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="SKU 名称" min-width="180" />
        <el-table-column label="规格" width="220">
          <template #default="{ row }">
            <span class="font-mono spec-text">{{ row.spec || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="售价" width="140">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原价" width="140">
          <template #default="{ row }">
            <span v-if="row.originalPrice" class="original-price">¥{{ row.originalPrice }}</span>
            <span v-else class="no-price">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const loading = ref(false)
const detail = ref(null)

const allImages = computed(() => {
  if (!detail.value) return []
  const imgs = []
  if (detail.value.spu.images) {
    try { imgs.push(...JSON.parse(detail.value.spu.images)) } catch (e) { /* ignore */ }
  }
  return imgs
})

function imageUrl(src) {
  if (!src) return ''
  if (src.startsWith('http')) return src
  return `/api/v1/files/${src}/url`
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await axios.get(`/api/v1/products/${route.params.id}`)
    if (data.code === 200) detail.value = data.data
  } finally { loading.value = false }
})
</script>

<style scoped>
.product-detail {
  max-width: 1100px;
}
.detail-header {
  margin-bottom: 20px;
}
.back-btn {
  border: none !important;
  padding: 8px 4px !important;
  color: var(--text-secondary) !important;
  transition: color var(--transition-fast);
}
.back-btn:hover {
  color: var(--accent) !important;
  background: transparent !important;
}

.detail-layout {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 28px;
  margin-bottom: 28px;
}

/* Images */
.image-gallery {
  position: sticky;
  top: 28px;
}
.main-image {
  width: 100%;
  aspect-ratio: 1;
  object-fit: cover;
  border-radius: var(--radius-xl);
  border: 1px solid var(--border-subtle);
  display: block;
  box-shadow: var(--shadow-sm);
}
.main-image.empty {
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--bg-surface);
  color: var(--text-muted);
  font-size: 15px;
  border: 2px dashed var(--border-default);
}
.image-thumbs {
  display: flex;
  gap: 10px;
  margin-top: 14px;
}
.thumb-img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border-radius: var(--radius);
  border: 1px solid var(--border-subtle);
  cursor: pointer;
  transition: all var(--transition-fast);
}
.thumb-img:hover {
  border-color: var(--accent);
  box-shadow: 0 0 0 2px rgba(99, 102, 241, 0.2);
}

/* Info */
.detail-info {
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.info-header {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 24px;
}
.product-title {
  font-size: 26px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
  line-height: 1.3;
}

.info-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  margin-bottom: 20px;
}
.info-item {
  padding: 14px 16px;
  background: var(--bg-surface);
  border-radius: var(--radius);
}
.info-label {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  margin-bottom: 4px;
}
.info-value {
  font-size: 15px;
  font-weight: 600;
  color: var(--text-primary);
}

.info-desc {
  margin-bottom: 24px;
}
.desc-text {
  font-size: 14px;
  color: var(--text-secondary);
  line-height: 1.7;
  margin-top: 6px;
}

.detail-actions {
  display: flex;
  gap: 10px;
}

/* Cards */
.detail-html-card,
.sku-card {
  margin-bottom: 28px;
}
.card-title {
  font-size: 16px;
  font-weight: 700;
  color: var(--text-primary);
}

.detail-html {
  line-height: 1.9;
  font-size: 14px;
  color: var(--text-secondary);
}
.detail-html :deep(img) {
  max-width: 100%;
  border-radius: var(--radius);
}

/* SKU */
.sku-thumb {
  width: 56px;
  height: 56px;
  object-fit: cover;
  border-radius: var(--radius-sm);
  border: 1px solid var(--border-subtle);
}
.no-image {
  color: var(--text-muted);
  font-size: 13px;
}
.spec-text {
  font-size: 12px;
  color: var(--text-secondary);
}
.price-text {
  font-weight: 700;
  font-size: 15px;
  color: var(--text-primary);
  font-family: var(--font-mono);
}
.original-price {
  font-size: 13px;
  color: var(--text-muted);
  text-decoration: line-through;
  font-family: var(--font-mono);
}
.no-price {
  color: var(--text-muted);
}
</style>
