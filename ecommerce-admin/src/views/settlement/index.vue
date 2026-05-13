<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">日终结算</span>
        <p class="page-desc">按日汇总支付和退款数据</p>
      </div>
      <el-button type="primary" size="large" @click="generateSettlement" :loading="generating">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 生成今日结算
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="结算日期" width="140">
          <template #default="{ row }">
            <span class="font-mono" style="font-weight:600">{{ row.settlementDate }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付笔数" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-num">{{ row.totalPaymentCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付总额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.totalPaymentAmount || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款笔数" width="100" align="center">
          <template #default="{ row }">
            <span class="stat-num">{{ row.totalRefundCount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="退款总额" width="140" align="right">
          <template #default="{ row }">
            <span class="amount-text" style="color:var(--red)">¥{{ row.totalRefundAmount || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="净收入" width="140" align="right">
          <template #default="{ row }">
            <span class="net-amount" :class="netClass(row.netAmount)">¥{{ row.netAmount || '0.00' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'" size="small">{{ row.statusText }}</el-tag>
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
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const generating = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

function netClass(val) {
  if (!val || val === '0.00') return ''
  return val.startsWith('-') ? 'negative' : 'positive'
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/settlements', {
      params: { page: page.value, size: size.value }
    })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = data.data?.total || 0
    }
  } finally { loading.value = false }
}

async function generateSettlement() {
  generating.value = true
  try {
    await axios.post('/api/v1/admin/settlements')
    ElMessage.success('结算报表生成成功')
    fetchData()
  } catch {
    ElMessage.error('生成失败')
  } finally { generating.value = false }
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
.amount-text { font-weight: 700; font-family: var(--font-mono); font-size: 14px; color: var(--text-primary); }
.net-amount { font-weight: 800; font-size: 16px; font-family: var(--font-mono); }
.net-amount.positive { color: var(--green); }
.net-amount.negative { color: var(--red); }
.pagination-wrap { margin-top: 20px; display: flex; justify-content: flex-end; }
</style>
