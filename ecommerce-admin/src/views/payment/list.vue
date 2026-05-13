<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">支付管理</span>
        <p class="page-desc">查看平台支付记录，处理退款</p>
      </div>
      <div class="toolbar-right">
        <el-select v-model="statusFilter" placeholder="支付状态" style="width:140px" clearable @change="statusFilterChange">
          <el-option label="已支付" :value="1" />
          <el-option label="已退款" :value="3" />
          <el-option label="退款中" :value="2" />
          <el-option label="已关闭" :value="4" />
        </el-select>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="支付单号" width="200">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.paymentNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="订单号" width="200">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.orderNo }}</span>
          </template>
        </el-table-column>
        <el-table-column label="用户 ID" width="180">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="120" align="right">
          <template #default="{ row }">
            <span class="amount-text">¥{{ row.amount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="支付方式" width="100" align="center">
          <template #default="{ row }">
            <span style="font-size:13px">{{ row.payMethod || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="支付时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.paidAt || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" @click="openRefund(row)">退款</el-button>
            <span v-else style="font-size:12px;color:var(--text-muted)">--</span>
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

    <el-dialog v-model="refundVisible" title="退款确认" width="440px">
      <div class="refund-info">
        <div class="refund-row"><span class="refund-label">支付单号</span><span class="font-mono">{{ refundForm.paymentNo }}</span></div>
        <div class="refund-row"><span class="refund-label">支付金额</span><span class="amount-text">¥{{ refundForm.amount }}</span></div>
        <div class="refund-row"><span class="refund-label">退款方式</span><span>全额退款</span></div>
      </div>
      <div style="margin-top:16px">
        <label style="font-size:13px;font-weight:600;color:var(--text-secondary);margin-bottom:8px;display:block">退款原因</label>
        <el-input v-model="refundReason" type="textarea" :rows="2" placeholder="选填" />
      </div>
      <template #footer>
        <el-button @click="refundVisible = false">取消</el-button>
        <el-button type="danger" @click="doRefund">确认退款</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref(null)

const refundVisible = ref(false)
const refundForm = ref({})
const refundReason = ref('')

function handleSizeChange() {
  page.value = 1
  fetchData()
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: '', 3: 'info', 4: 'danger' }
  return map[status] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/payment', {
      params: { status: statusFilter.value ?? undefined, page: page.value, size: size.value }
    })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = Number(data.data?.total) || tableData.value.length
    }
  } finally { loading.value = false }
}

function statusFilterChange() {
  page.value = 1
  fetchData()
}

function openRefund(row) {
  refundForm.value = { ...row }
  refundReason.value = ''
  refundVisible.value = true
}

async function doRefund() {
  try {
    await axios.post(`/api/v1/admin/payment/${refundForm.value.orderNo}/refund`, {
      reason: refundReason.value
    })
    ElMessage.success('退款成功')
    refundVisible.value = false
    fetchData()
  } catch { ElMessage.error('退款失败') }
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
.toolbar-right { display: flex; gap: 12px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
.id-text { font-size: 12px; color: var(--text-muted); }
.amount-text { font-weight: 700; color: var(--text-primary); font-family: var(--font-mono); }
.time-text { font-size: 12px; color: var(--text-secondary); }
.refund-info { background: var(--bg-surface); border-radius: var(--radius); padding: 16px 20px; }
.refund-row { display: flex; padding: 6px 0; justify-content: space-between; align-items: center; }
.refund-label { font-size: 13px; font-weight: 600; color: var(--text-secondary); }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
