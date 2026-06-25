<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">订单管理</span>
        <p class="page-desc">查看和管理平台所有订单</p>
      </div>
      <div class="toolbar-right">
        <el-select v-model="statusFilter" placeholder="订单状态" style="width:140px" clearable @change="fetchData">
          <el-option label="待支付" :value="0" />
          <el-option label="已支付" :value="1" />
          <el-option label="已发货" :value="2" />
          <el-option label="已完成" :value="3" />
          <el-option label="已取消" :value="4" />
        </el-select>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
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
            <span class="amount-text">¥{{ row.totalAmount }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="收货人" width="140">
          <template #default="{ row }">
            <div>
              <div style="font-size:13px;color:var(--text-primary)">{{ row.receiverName }}</div>
              <div style="font-size:11px;color:var(--text-muted)">{{ row.receiverPhone }}</div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="180">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="warning" @click="openShipDialog(row)">发货</el-button>
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

    <!-- 发货弹窗 -->
    <el-dialog v-model="shipDialogVisible" title="订单发货" width="500px">
      <el-form :model="shipForm" label-width="100px">
        <el-form-item label="订单号"><span>{{ shipForm.orderNo }}</span></el-form-item>
        <el-form-item label="物流公司" required>
          <el-select v-model="shipForm.providerId" placeholder="选择物流公司">
            <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="运单号" required>
          <el-input v-model="shipForm.trackingNo" placeholder="输入运单号" />
        </el-form-item>
        <el-form-item label="包裹重量(克)">
          <el-input-number v-model="shipForm.packageWeight" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="shipDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="shipSubmitting" @click="submitShip">确认发货</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const statusFilter = ref(null)
const page = ref(1)
const size = ref(10)
const total = ref(0)

// 发货弹窗状态
const shipDialogVisible = ref(false)
const shipSubmitting = ref(false)
const providers = ref([])
const shipForm = reactive({
  orderId: null,
  orderNo: '',
  providerId: null,
  trackingNo: '',
  packageWeight: 0,
  clientRequestId: '',
  items: []
})

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
    const { data } = await axios.get('/api/v1/admin/orders', {
      params: { page: page.value, size: size.value, status: statusFilter.value ?? undefined }
    })
    if (data.code === 200) {
      tableData.value = data.data.records || []
      total.value = Number(data.data.total) || tableData.value.length
    }
  } finally { loading.value = false }
}

const openShipDialog = async (row) => {
  // 加载物流公司列表
  if (providers.value.length === 0) {
    const { data } = await axios.get('/api/v1/admin/logistics/providers/all')
    if (data.code === 200) providers.value = data.data || []
  }
  shipForm.orderId = row.id
  shipForm.orderNo = row.orderNo
  shipForm.providerId = null
  shipForm.trackingNo = ''
  shipForm.packageWeight = 0
  shipForm.clientRequestId = 'ship-' + row.id + '-' + Date.now()
  shipForm.items = (row.items || []).map(item => ({
    orderItemId: item.id, skuId: item.skuId, quantity: item.quantity
  }))
  shipDialogVisible.value = true
}

const submitShip = async () => {
  if (!shipForm.providerId || !shipForm.trackingNo) {
    ElMessage.warning('请选择物流公司并填写运单号')
    return
  }
  shipSubmitting.value = true
  try {
    const { data } = await axios.post('/api/v1/admin/logistics/shipping', {
      clientRequestId: shipForm.clientRequestId,
      orderId: shipForm.orderId,
      providerId: shipForm.providerId,
      trackingNo: shipForm.trackingNo,
      packageWeight: shipForm.packageWeight,
      items: shipForm.items
    })
    if (data.code === 200) {
      ElMessage.success('发货成功')
      shipDialogVisible.value = false
      fetchData()
    } else {
      ElMessage.error(data.message || '发货失败')
    }
  } catch (e) {
    ElMessage.error('发货失败')
  } finally { shipSubmitting.value = false }
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
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
.id-text { font-size: 12px; color: var(--text-muted); }
.amount-text { font-weight: 700; color: var(--text-primary); font-family: var(--font-mono); }
.time-text { font-size: 12px; color: var(--text-secondary); }
</style>
