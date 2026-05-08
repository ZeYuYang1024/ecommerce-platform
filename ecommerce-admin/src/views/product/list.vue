<template>
  <div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="keyword" placeholder="搜索商品..." style="width:280px" clearable @clear="fetchData" />
        <el-select v-model="status" placeholder="状态" style="width:140px" clearable @change="fetchData">
          <el-option label="上架" :value="1" />
          <el-option label="下架" :value="0" />
        </el-select>
      </div>
      <el-button type="primary" @click="$router.push('/products/create')">
        <el-icon style="margin-right:4px"><Plus /></el-icon> 新增商品
      </el-button>
    </div>

    <el-table :data="tableData" v-loading="loading" class="data-table">
      <el-table-column prop="id" label="ID" width="180">
        <template #default="{ row }">
          <span class="font-mono" style="font-size:12px;color:var(--text-muted)">{{ row.id }}</span>
        </template>
      </el-table-column>
      <el-table-column label="商品" min-width="240">
        <template #default="{ row }">
          <div style="display:flex;align-items:center;gap:12px">
            <div v-if="row.mainImage" class="thumb" :style="{ backgroundImage: `url(${thumbUrl(row.mainImage)})` }"></div>
            <div v-else class="thumb thumb-empty">
              <el-icon :size="16"><Picture /></el-icon>
            </div>
            <span style="font-weight:500">{{ row.name }}</span>
          </div>
        </template>
      </el-table-column>
      <el-table-column label="状态" width="100" align="center">
        <template #default="{ row }">
          <el-switch :model-value="row.status === 1" @change="toggleStatus(row)" size="small" />
        </template>
      </el-table-column>
      <el-table-column label="操作" width="180" align="center">
        <template #default="{ row }">
          <el-button size="small" @click="$router.push(`/products/${row.id}`)">查看</el-button>
          <el-button size="small" @click="$router.push(`/products/${row.id}/edit`)">编辑</el-button>
          <el-popconfirm title="确认删除？" @confirm="handleDelete(row.id)">
            <template #reference><el-button size="small" class="btn-danger">删除</el-button></template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        layout="total, prev, pager, next"
        @current-change="fetchData"
      />
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const keyword = ref('')
const status = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)

function thumbUrl(src) {
  if (!src) return ''
  if (src.startsWith('http')) return src
  return `/api/v1/files/${src}/url`
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/products', {
      params: { page: page.value, size: size.value, keyword: keyword.value || undefined, status: status.value }
    })
    if (data.code === 200) {
      tableData.value = data.data.records
      total.value = data.data.total
    }
  } finally { loading.value = false }
}

async function toggleStatus(row) {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/v1/admin/products/${row.id}/status`, { status: newStatus })
  row.status = newStatus
}

async function handleDelete(id) {
  await axios.delete(`/api/v1/admin/products/${id}`)
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.toolbar-left { display: flex; gap: 10px; }
.font-mono { font-family: var(--font-mono); }

.thumb {
  width: 48px; height: 48px;
  border-radius: var(--radius-sm);
  background-size: cover;
  background-position: center;
  flex-shrink: 0;
}
.thumb-empty {
  background: var(--bg-surface);
  border: 1px dashed var(--border-default);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--text-muted);
}

.btn-danger {
  --el-button-text-color: var(--red);
  --el-button-hover-text-color: #fff;
  --el-button-hover-bg-color: var(--red);
  --el-button-hover-border-color: var(--red);
}

.pagination-wrap {
  margin-top: 16px;
  display: flex;
  justify-content: flex-end;
}
</style>
