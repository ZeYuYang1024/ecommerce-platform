<template>
  <div class="shop-page">
    <div class="toolbar">
      <div>
        <span class="section-label">店铺信息</span>
        <p class="page-desc">查看和编辑店铺基本信息</p>
      </div>
    </div>

    <el-card shadow="never" class="info-card" v-loading="loading">
      <div v-if="shop" class="shop-info">
        <div class="shop-hero">
          <div class="shop-logo">{{ shop.name?.charAt(0) }}</div>
          <div>
            <h2>{{ shop.name }}</h2>
            <el-tag :type="statusTag(shop.status)" size="small">{{ shop.statusText }}</el-tag>
          </div>
        </div>
        <el-descriptions :column="2" border class="info-table">
          <el-descriptions-item label="商家 ID">{{ shop.id }}</el-descriptions-item>
          <el-descriptions-item label="联系人">{{ shop.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ shop.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ shop.createdAt }}</el-descriptions-item>
          <el-descriptions-item v-if="shop.reason" label="备注" :span="2">{{ shop.reason }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="empty">暂无店铺信息</div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const shop = ref(null)

function statusTag(s) { const m = {0:'warning',1:'success',2:'danger',3:'info'}; return m[s]||'info' }

onMounted(async () => {
  loading.value = true
  try {
    const merchantId = localStorage.getItem('merchantId')
    if (merchantId) {
      const { data } = await axios.get(`/api/v1/admin/merchants/${merchantId}`)
      if (data.code === 200) shop.value = data.data
    }
  } finally { loading.value = false }
})
</script>

<style scoped>
.shop-page { max-width: 720px; }
.toolbar { margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.info-card { border-radius: var(--radius-xl); }
.shop-hero { display: flex; align-items: center; gap: 20px; margin-bottom: 28px; }
.shop-logo { width: 64px; height: 64px; border-radius: var(--radius-xl); background: linear-gradient(135deg,#059669,#10b981); color:#fff; display: flex; align-items: center; justify-content: center; font-size: 28px; font-weight: 700; }
.shop-hero h2 { font-size: 22px; font-weight: 700; margin-bottom: 4px; }
.info-table { margin-top: 8px; }
.empty { text-align: center; padding: 60px; color: var(--text-muted); }
</style>
