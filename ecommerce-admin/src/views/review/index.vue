<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">评论管理</span>
        <p class="page-desc">查看和管理商品评论</p>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" size="small">
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
            <span class="font-mono time-text">{{ formatDateTime(row.createdAt) }}</span>
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

      <div class="pagination-row">
              <el-pagination
                v-model:current-page="page"
                v-model:page-size="size"
                :page-sizes="[10, 20, 50, 100]"
                :total="total"
                layout="total, sizes, prev, pager, next, jumper"
                @size-change="handleSizeChange"
                @current-change="fetchData"
              />
            </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { formatDateTime } from '@/utils/dateTime'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const isMerchantView = localStorage.getItem('type') === 'merchant'
const listUrl = isMerchantView ? '/api/v1/admin/merchant/reviews' : '/api/v1/admin/reviews'
const deleteBaseUrl = isMerchantView ? '/api/v1/admin/merchant/reviews' : '/api/v1/admin/reviews'

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get(listUrl, {
      params: { page: page.value, size: size.value }
    })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = Number(data.data?.total) || tableData.value.length
    }
  } finally { loading.value = false }
}

async function doDelete(id) {
  await axios.delete(`${deleteBaseUrl}/${id}`)
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
.time-text { font-size: 12px; color: var(--text-secondary); white-space: nowrap; display: inline-block; }
.table-card :deep(.el-table .cell) { line-height: 1.5; }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
