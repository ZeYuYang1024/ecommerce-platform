<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">订单管理</span>
        <p class="page-desc">查看您店铺商品的订单</p>
      </div>
      <el-select v-model="statusFilter" placeholder="状态" style="width:140px" clearable @change="fetchData">
        <el-option label="待支付" :value="0" /><el-option label="已支付" :value="1" />
        <el-option label="已发货" :value="2" /><el-option label="已完成" :value="3" />
      </el-select>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="订单号" width="200">
          <template #default="{ row }"><span class="font-mono" style="font-size:12px">{{ row.orderNo }}</span></template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }"><span class="amount-text">¥{{ row.totalAmount }}</span></template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }"><el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag></template>
        </el-table-column>
        <el-table-column label="收货人" width="140">
          <template #default="{ row }"><span style="font-size:13px">{{ row.receiverName }}</span></template>
        </el-table-column>
        <el-table-column label="时间" width="180">
          <template #default="{ row }"><span class="time-text">{{ row.createdAt }}</span></template>
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

const loading = ref(false)
const tableData = ref([])
const statusFilter = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)

function handleSizeChange() {
  page.value = 1
  fetchData()
}

function statusType(s) { const m = {0:'warning',1:'success',2:'',3:'info',4:'danger'}; return m[s]||'info' }

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (statusFilter.value !== null && statusFilter.value !== '') params.status = statusFilter.value
    const { data } = await axios.get('/api/v1/admin/merchant/orders', { params })
    if (data.code === 200) { tableData.value = data.data.records || []; total.value = Number(data.data.total) || tableData.value.length }
  } finally { loading.value = false }
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
.amount-text { font-weight: 700; color: var(--text-primary); }
.time-text { font-size: 12px; color: var(--text-secondary); }
</style>
