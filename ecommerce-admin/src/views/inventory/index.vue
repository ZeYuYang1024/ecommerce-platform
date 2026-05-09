<template>
  <div>
    <div class="page-header">
      <h1 class="section-label">库存管理</h1>
      <p class="page-desc">查询和管理商品 SKU 的库存数量</p>
    </div>

    <el-card shadow="never" class="search-card">
      <div class="lookup-bar">
        <el-input v-model="skuId" placeholder="输入 SKU ID 查询" style="width:320px" size="large" clearable @keyup.enter="search">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button type="primary" size="large" @click="search" :loading="loading">
          <el-icon style="margin-right:6px"><Search /></el-icon> 查询
        </el-button>
      </div>
    </el-card>

    <div v-if="stock" class="stock-display">
      <el-card shadow="never" class="stock-card">
        <div class="stock-grid">
          <div class="stock-field">
            <span class="stock-label">SKU ID</span>
            <span class="stock-value font-mono id-val">{{ stock.skuId }}</span>
          </div>
          <div class="stock-field">
            <span class="stock-label">总库存</span>
            <span class="stock-value font-mono">{{ stock.totalStock }}</span>
          </div>
          <div class="stock-field">
            <span class="stock-label">已锁定</span>
            <span class="stock-value font-mono locked">{{ stock.lockedStock }}</span>
          </div>
          <div class="stock-field">
            <span class="stock-label">可售</span>
            <span class="stock-value font-mono available">{{ stock.availableStock }}</span>
          </div>
        </div>
        <div class="stock-divider"></div>
        <div class="stock-update">
          <span class="update-label">更新总库存</span>
          <div class="update-row">
            <el-input-number v-model="newStock" :min="0" size="large" :precision="0" style="width:180px" />
            <el-button type="primary" size="large" @click="updateStock" :loading="saving">更新库存</el-button>
          </div>
        </div>
      </el-card>
    </div>

    <div v-else-if="searched" class="empty-state">
      <div class="empty-icon">
        <el-icon :size="48"><Search /></el-icon>
      </div>
      <h3>未找到库存记录</h3>
      <p>该 SKU ID 尚未创建库存，请先使用"更新库存"功能初始化</p>
      <el-button type="primary" @click="setNewStock" style="margin-top:12px">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 创建库存
      </el-button>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const skuId = ref('')
const stock = ref(null)
const searched = ref(false)
const loading = ref(false)
const saving = ref(false)
const newStock = ref(0)

async function search() {
  if (!skuId.value.trim()) return
  searched.value = true
  loading.value = true
  try {
    const { data } = await axios.get(`/api/v1/inventory/${skuId.value}`)
    if (data.code === 200) {
      stock.value = data.data
      newStock.value = data.data.totalStock
    }
  } catch {
    stock.value = null
    newStock.value = 0
  } finally {
    loading.value = false
  }
}

async function updateStock() {
  if (!stock.value) return
  saving.value = true
  try {
    await axios.post(`/api/v1/admin/inventory/${stock.value.skuId}`, { totalStock: newStock.value })
    ElMessage.success('库存更新成功')
    search()
  } catch {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

function setNewStock() {
  stock.value = { skuId: skuId.value, totalStock: 0, lockedStock: 0, availableStock: 0 }
  newStock.value = 0
}
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.page-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.search-card {
  margin-bottom: 24px;
}
.lookup-bar {
  display: flex;
  gap: 12px;
  align-items: center;
}

.stock-display {
  max-width: 680px;
}
.stock-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 24px;
  padding: 4px 0;
}
.stock-label {
  display: block;
  font-size: 11.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.06em;
  color: var(--text-muted);
  margin-bottom: 8px;
}
.stock-value {
  font-size: 28px;
  font-weight: 800;
  color: var(--text-primary);
}
.stock-value.locked { color: var(--accent); }
.stock-value.available { color: var(--green); }
.id-val { font-size: 18px; }

.stock-divider {
  height: 1px;
  background: var(--border-subtle);
  margin: 24px 0;
}
.stock-update {
  display: flex;
  flex-direction: column;
  gap: 10px;
}
.update-label {
  font-size: 12px;
  font-weight: 600;
  color: var(--text-secondary);
  text-transform: uppercase;
  letter-spacing: 0.05em;
}
.update-row {
  display: flex;
  gap: 12px;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 80px 0;
}
.empty-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 80px;
  height: 80px;
  border-radius: var(--radius-2xl);
  background: var(--bg-surface);
  color: var(--text-muted);
  margin-bottom: 16px;
}
.empty-state h3 {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 6px;
}
.empty-state p {
  font-size: 14px;
  color: var(--text-muted);
}
</style>
