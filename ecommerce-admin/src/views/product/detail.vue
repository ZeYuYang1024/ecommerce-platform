<template>
  <div class="product-detail" v-loading="loading">
    <div class="detail-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
    </div>

    <el-card v-if="detail" class="detail-card">
      <!-- 图片区域 -->
      <div class="images-section">
        <img v-if="detail.spu.mainImage" :src="imageUrl(detail.spu.mainImage)" class="main-image" />
        <div v-else class="main-image empty">暂无主图</div>
        <div v-if="allImages.length > 0" class="image-list">
          <img v-for="(img, i) in allImages" :key="i" :src="imageUrl(img)" class="sub-image" />
        </div>
      </div>

      <!-- 基本信息 -->
      <el-descriptions :column="2" border class="info-table">
        <el-descriptions-item label="商品 ID">{{ detail.spu.id }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="detail.spu.status === 1 ? 'success' : 'info'">{{ detail.spu.status === 1 ? '上架' : '下架' }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="商品名称" :span="2">{{ detail.spu.name }}</el-descriptions-item>
        <el-descriptions-item label="简介" :span="2">{{ detail.spu.description || '暂无' }}</el-descriptions-item>
        <el-descriptions-item label="平均评分">{{ detail.spu.avgRating || '-' }}</el-descriptions-item>
        <el-descriptions-item label="评论数">{{ detail.spu.reviewCount || 0 }}</el-descriptions-item>
        <el-descriptions-item label="创建时间" :span="2">{{ detail.spu.createdAt }}</el-descriptions-item>
      </el-descriptions>

      <!-- 商品详情 -->
      <div v-if="detail.spu.detail" class="detail-html" v-html="detail.spu.detail"></div>

      <!-- SKU 列表 -->
      <div class="section-title">SKU 列表</div>
      <el-table :data="detail.skus" class="data-table" v-if="detail.skus && detail.skus.length > 0">
        <el-table-column label="图片" width="90">
          <template #default="{ row }">
            <img v-if="row.image" :src="imageUrl(row.image)" class="sku-thumb" />
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="SKU 名称" />
        <el-table-column label="规格" width="200">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.spec || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="price" label="售价" width="120">
          <template #default="{ row }">
            <span class="font-mono" style="font-weight:700">¥{{ row.price }}</span>
          </template>
        </el-table-column>
        <el-table-column label="原价" width="120">
          <template #default="{ row }">
            <span v-if="row.originalPrice" class="font-mono" style="text-decoration:line-through;color:var(--text-muted)">¥{{ row.originalPrice }}</span>
            <span v-else style="color:var(--text-muted)">-</span>
          </template>
        </el-table-column>
      </el-table>

      <div class="form-actions">
        <el-button type="primary" @click="$router.push(`/products/${detail.spu.id}/edit`)">编辑商品</el-button>
        <el-button @click="$router.back()">返回</el-button>
      </div>
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
.product-detail { max-width: 900px; }
.detail-header { margin-bottom: 16px; }
.detail-card { padding: 0; }

.images-section { margin-bottom: 24px; }
.main-image { max-width: 400px; max-height: 400px; object-fit: cover; border: 2px solid var(--border-default); display: block; }
.main-image.empty { width: 400px; height: 300px; display: flex; align-items: center; justify-content: center; background: var(--bg-surface); color: var(--text-muted); border: 2px dashed var(--border-default); }
.image-list { display: flex; gap: 8px; margin-top: 12px; }
.sub-image { width: 80px; height: 80px; object-fit: cover; border: 2px solid var(--border-subtle); }
.sub-image:hover { border-color: var(--border-default); }

.info-table { margin-bottom: 24px; }

.detail-html { padding: 20px; background: var(--bg-surface); border: 1px solid var(--border-subtle); margin-bottom: 24px; line-height: 1.8; }
.detail-html :deep(img) { max-width: 100%; }

.section-title { font-size: 14px; font-weight: 700; margin-bottom: 12px; color: var(--text-primary); }
.sku-thumb { width: 60px; height: 60px; object-fit: cover; border: 1px solid var(--border-subtle); }
.font-mono { font-family: var(--font-mono); }
.form-actions { margin-top: 24px; display: flex; gap: 10px; }
</style>
