<template>
  <div>
    <div class="lookup-bar">
      <el-input v-model="skuId" placeholder="输入 SKU ID 查询" style="width:280px" clearable>
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="search">查询</el-button>
    </div>

    <div v-if="stock" class="stock-card">
      <div class="stock-row">
        <div class="stock-field">
          <span class="stock-label">SKU ID</span>
          <span class="stock-value font-mono">{{ stock.skuId }}</span>
        </div>
        <div class="stock-field">
          <span class="stock-label">总库存</span>
          <span class="stock-value font-mono">{{ stock.totalStock }}</span>
        </div>
        <div class="stock-field">
          <span class="stock-label">已锁定</span>
          <span class="stock-value font-mono" style="color:var(--accent)">{{ stock.lockedStock }}</span>
        </div>
        <div class="stock-field">
          <span class="stock-label">可售</span>
          <span class="stock-value font-mono" style="color:var(--green)">{{ stock.availableStock }}</span>
        </div>
      </div>
      <div class="stock-update">
        <el-input-number v-model="newStock" :min="0" size="large" />
        <el-button type="primary" @click="updateStock" size="large">更新库存</el-button>
      </div>
    </div>

    <div v-else-if="searched" class="empty-state">
      <el-icon :size="40" color="var(--text-muted)"><Search /></el-icon>
      <p>未找到该 SKU 的库存记录</p>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'

const skuId = ref('')
const stock = ref(null)
const searched = ref(false)
const newStock = ref(0)

async function search() {
  searched.value = true
  try {
    const { data } = await axios.get(`/api/v1/inventory/${skuId.value}`)
    if (data.code === 200) {
      stock.value = data.data
      newStock.value = data.data.totalStock
    }
  } catch { stock.value = null }
}

async function updateStock() {
  await axios.post(`/api/v1/admin/inventory/${stock.value.skuId}`, { totalStock: newStock.value })
  search()
}
</script>

<style scoped>
.lookup-bar {
  display: flex;
  gap: 10px;
  margin-bottom: 24px;
}
.font-mono { font-family: var(--font-mono); }

.stock-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 28px;
  max-width: 600px;
}
.stock-row {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 20px;
  margin-bottom: 24px;
  padding-bottom: 24px;
  border-bottom: 1px solid var(--border-subtle);
}
.stock-label {
  display: block;
  font-size: 11px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.1em;
  color: var(--text-muted);
  margin-bottom: 6px;
}
.stock-value {
  font-size: 22px;
  font-weight: 700;
  color: var(--text-primary);
}
.stock-update {
  display: flex;
  gap: 10px;
  align-items: center;
}

.empty-state {
  text-align: center;
  padding: 60px 0;
  color: var(--text-muted);
}
.empty-state p { margin-top: 12px; font-size: 14px; }
</style>
