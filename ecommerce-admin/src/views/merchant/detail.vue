<template>
  <div class="merchant-detail" v-loading="loading">
    <div class="detail-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
    </div>

    <el-card v-if="merchant" class="detail-card">
      <div class="merchant-hero">
        <div class="hero-logo">
          <img v-if="merchant.logo" :src="merchant.logo" />
          <span v-else class="logo-fallback">{{ merchant.name?.charAt(0) }}</span>
        </div>
        <div>
          <h2>{{ merchant.name }}</h2>
          <el-tag :type="statusType(merchant.status)" size="small" style="margin-top:4px">{{ merchant.statusText }}</el-tag>
        </div>
      </div>

      <el-descriptions :column="2" border class="info-table">
        <el-descriptions-item label="商家 ID">{{ merchant.id }}</el-descriptions-item>
        <el-descriptions-item label="联系人">{{ merchant.contactName }}</el-descriptions-item>
        <el-descriptions-item label="联系电话">{{ merchant.contactPhone }}</el-descriptions-item>
        <el-descriptions-item label="入驻时间">{{ merchant.createdAt }}</el-descriptions-item>
        <el-descriptions-item v-if="merchant.businessLicense" label="营业执照" :span="2">
          <a :href="merchant.businessLicense" target="_blank" class="license-link">查看营业执照</a>
        </el-descriptions-item>
        <el-descriptions-item v-if="merchant.reason" label="审核/关停原因" :span="2">
          <span style="color:var(--text-secondary)">{{ merchant.reason }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <div class="detail-actions" v-if="merchant.status === 0">
        <el-input v-model="auditComment" type="textarea" :rows="2" placeholder="审核意见（驳回时必填）" style="margin-bottom:12px" />
        <el-button type="danger" @click="doAudit(2)">驳回</el-button>
        <el-button type="primary" @click="doAudit(1)">通过审核</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const loading = ref(false)
const merchant = ref(null)
const auditComment = ref('')

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

onMounted(async () => {
  loading.value = true
  try {
    const { data } = await axios.get(`/api/v1/admin/merchants/${route.params.id}`)
    if (data.code === 200) merchant.value = data.data
  } finally { loading.value = false }
})

async function doAudit(action) {
  await axios.put(`/api/v1/admin/merchants/${route.params.id}/audit`, {
    action,
    comment: auditComment.value
  })
  // refresh
  const { data } = await axios.get(`/api/v1/admin/merchants/${route.params.id}`)
  if (data.code === 200) merchant.value = data.data
}
</script>

<style scoped>
.merchant-detail { max-width: 700px; }
.detail-header { margin-bottom: 16px; }
.detail-card { padding: 0; }

.merchant-hero {
  display: flex;
  align-items: center;
  gap: 20px;
  padding: 28px;
  border-bottom: 1px solid var(--border-subtle);
}
.hero-logo {
  width: 72px; height: 72px;
  border-radius: var(--radius-lg);
  overflow: hidden;
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
}
.hero-logo img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.logo-fallback {
  font-size: 28px;
  font-weight: 700;
  color: var(--text-muted);
}
.merchant-hero h2 {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.info-table {
  margin: 24px 28px;
}

.license-link {
  color: var(--accent);
  font-weight: 500;
  text-decoration: none;
}
.license-link:hover { text-decoration: underline; }

.detail-actions {
  padding: 20px 28px;
  border-top: 1px solid var(--border-subtle);
  text-align: right;
}
.detail-actions .el-button { margin-left: 10px; }
</style>
