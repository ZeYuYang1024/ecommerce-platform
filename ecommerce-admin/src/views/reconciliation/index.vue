<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">对账管理</span>
        <p class="page-desc">比对支付记录与订单记录，发现差异</p>
      </div>
      <el-button type="primary" size="large" @click="runRecon" :loading="running">
        <el-icon style="margin-right:6px"><RefreshRight /></el-icon> 执行对账
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="批次号" min-width="200">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.batchNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单总数" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-num">{{ row.totalOrderCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付单总数" width="110" align="center">
          <template #default="{ row }">
            <span class="stat-num">{{ row.totalPaymentCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="匹配成功" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-num" style="color:var(--green)">{{ row.matchedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="差异数" width="100" align="center">
          <template #default="{ row }">
            <span :class="row.unmatchedCount > 0 ? 'stat-num bad' : 'stat-num'">{{ row.unmatchedCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="100" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/reconciliation/${row.id}`)">查看明细</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const running = ref(false)
const tableData = ref([])

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/reconciliation')
    if (data.code === 200) tableData.value = data.data || []
  } finally { loading.value = false }
}

async function runRecon() {
  running.value = true
  try {
    const { data } = await axios.post('/api/v1/admin/reconciliation/run')
    if (data.code === 200) {
      ElMessage.success(`对账完成：匹配 ${data.data.matchedCount} 笔，差异 ${data.data.unmatchedCount} 笔`)
    }
    fetchData()
  } catch {
    ElMessage.error('对账执行失败')
  } finally { running.value = false }
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
.stat-num { font-weight: 700; font-size: 15px; font-family: var(--font-mono); }
.stat-num.bad { color: var(--red); }
.time-text { font-size: 12px; color: var(--text-secondary); }
</style>
