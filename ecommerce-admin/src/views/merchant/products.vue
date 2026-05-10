<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">商品管理</span>
        <p class="page-desc">管理您店铺的商品</p>
      </div>
      <el-button type="primary" @click="$router.push('/products/create')">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 新增商品
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="id" label="ID" width="180">
          <template #default="{ row }"><span class="font-mono id-text">{{ row.id }}</span></template>
        </el-table-column>
        <el-table-column prop="name" label="商品名称" min-width="200" />
        <el-table-column label="价格" width="140">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.minPrice || '--' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-pagination
      class="pagination"
      v-model:current-page="page"
      v-model:page-size="size"
      :total="total"
      layout="total, prev, pager, next"
      @current-change="fetchData"
    />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/products', { params: { page: page.value, size: size.value } })
    if (data.code === 200) { tableData.value = data.data.records || []; total.value = data.data.total || 0 }
  } finally { loading.value = false }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/v1/admin/products/${row.id}/status`, { status: newStatus })
  row.status = newStatus
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
.pagination { justify-content: flex-end; margin-top: 16px; }
.id-text { font-size: 12px; color: var(--text-muted); }
.price-text { font-weight: 700; color: var(--text-primary); }
</style>
