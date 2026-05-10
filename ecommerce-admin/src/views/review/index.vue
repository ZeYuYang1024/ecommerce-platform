<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">评论管理</span>
        <p class="page-desc">查看和管理商品评论</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="id" label="ID" width="180">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="内容" min-width="240">
          <template #default="{ row }">
            <div>
              <div style="font-size:13px;color:var(--text-primary);margin-bottom:2px">{{ row.content || '(无内容)' }}</div>
              <div style="font-size:11px;color:var(--text-muted)">
                SPU: {{ row.spuId }} · 用户: {{ row.username || row.userId }} · {{ row.rating }}星
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="评分" width="80" align="center">
          <template #default="{ row }">
            <span style="color:var(--accent);font-weight:600">{{ row.rating }} / 5</span>
          </template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/reviews')
    if (data.code === 200) tableData.value = data.data || []
  } finally { loading.value = false }
}

async function doDelete(id) {
  await axios.delete(`/api/v1/admin/reviews/${id}`)
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
.id-text { font-size: 12px; color: var(--text-muted); }
.time-text { font-size: 12px; color: var(--text-secondary); }
</style>
