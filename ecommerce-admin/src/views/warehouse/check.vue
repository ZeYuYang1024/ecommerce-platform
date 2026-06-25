<template>
  <div class="check-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>盘点管理</span>
          <el-button type="primary" @click="showCreateDialog">新增盘点单</el-button>
        </div>
      </template>
      <el-form :inline="true" :model="query" @submit.prevent="fetchData">
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable>
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="盘点中" :value="0" />
            <el-option label="已完成" :value="1" />
            <el-option label="已取消" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="checkNo" label="盘点单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="150" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="checkStatusTag(row.status)">{{ checkStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="220" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showDetail(row)">查看明细</el-button>
            <el-button v-if="row.status === 0" size="small" type="success" @click="completeCheck(row)">完成盘点</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>

    <!-- 创建盘点单弹窗 -->
    <el-dialog v-model="dialogVisible" title="新增盘点单" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库" required>
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="盘点备注" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitCreate">确认创建</el-button>
      </template>
    </el-dialog>

    <!-- 盘点明细弹窗 -->
    <el-dialog v-model="detailVisible" :title="`盘点明细 - ${currentCheckNo}`" width="800px">
      <el-table :data="detailItems" v-loading="detailLoading" stripe>
        <el-table-column prop="skuId" label="SKU ID" width="100" />
        <el-table-column prop="skuName" label="SKU名称" width="150" />
        <el-table-column prop="binCode" label="库位" width="120" />
        <el-table-column prop="systemQty" label="系统数量" width="100" />
        <el-table-column prop="actualQty" label="实盘数量" width="100">
          <template #default="{ row }">
            <span v-if="row.actualQty != null">{{ row.actualQty }}</span>
            <el-tag v-else type="info" size="small">未盘点</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="差异" width="80">
          <template #default="{ row }">
            <span v-if="row.actualQty != null"
              :style="{ color: row.actualQty - row.systemQty !== 0 ? '#f56c6c' : '#67c23a', fontWeight: 'bold' }">
              {{ row.actualQty - row.systemQty > 0 ? '+' : '' }}{{ row.actualQty - row.systemQty }}
            </span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button v-if="currentCheckStatus === 0" size="small" @click="showRecordDialog(row)">录入实盘</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-dialog>

    <!-- 录入实盘数量弹窗 -->
    <el-dialog v-model="recordVisible" title="录入实盘数量" width="400px">
      <el-form :model="recordForm" label-width="100px">
        <el-form-item label="SKU">
          <span>{{ recordForm.skuName || recordForm.skuId }}</span>
        </el-form-item>
        <el-form-item label="系统数量">
          <span>{{ recordForm.systemQty }}</span>
        </el-form-item>
        <el-form-item label="实盘数量" required>
          <el-input-number v-model="recordForm.actualQty" :min="0" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="recordVisible = false">取消</el-button>
        <el-button type="primary" @click="submitRecord">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const warehouseOptions = ref([])
const query = reactive({ warehouseId: null, status: null })

const dialogVisible = ref(false)
const form = reactive({ warehouseId: null, remark: '' })

const detailVisible = ref(false)
const detailLoading = ref(false)
const detailItems = ref([])
const currentCheckNo = ref('')
const currentCheckId = ref(null)
const currentCheckStatus = ref(null)

const recordVisible = ref(false)
const recordForm = reactive({ checkId: null, itemId: null, skuId: '', skuName: '', systemQty: 0, actualQty: 0 })

const checkStatusText = (status) => {
  return ['盘点中', '已完成', '已取消'][status] || '未知'
}

const checkStatusTag = (status) => {
  return status === 0 ? 'warning' : status === 1 ? 'success' : 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/warehouse/checks', {
      params: { page: currentPage.value, size: pageSize.value, ...query }
    })
    if (data.code === 200) {
      tableData.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } finally { loading.value = false }
}

const fetchWarehouseOptions = async () => {
  try {
    const { data } = await axios.get('/api/v1/admin/warehouses', { params: { page: 1, size: 999 } })
    if (data.code === 200) {
      warehouseOptions.value = data.data.records || []
    }
  } catch { /* ignore */ }
}

const showCreateDialog = () => {
  Object.assign(form, { warehouseId: null, remark: '' })
  dialogVisible.value = true
}

const submitCreate = async () => {
  try {
    await axios.post('/api/v1/admin/warehouse/checks', form)
    ElMessage.success('盘点单创建成功，已自动生成盘点明细')
    dialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error('操作失败') }
}

const showDetail = async (row) => {
  currentCheckNo.value = row.checkNo
  currentCheckId.value = row.id
  currentCheckStatus.value = row.status
  detailVisible.value = true
  detailLoading.value = true
  try {
    const { data } = await axios.get(`/api/v1/admin/warehouse/checks/${row.id}`)
    if (data.code === 200) {
      detailItems.value = data.data.items || data.data || []
    }
  } catch { ElMessage.error('获取明细失败') }
  finally { detailLoading.value = false }
}

const showRecordDialog = (row) => {
  Object.assign(recordForm, {
    checkId: currentCheckId.value,
    itemId: row.id,
    skuId: row.skuId,
    skuName: row.skuName,
    systemQty: row.systemQty,
    actualQty: row.actualQty != null ? row.actualQty : row.systemQty
  })
  recordVisible.value = true
}

const submitRecord = async () => {
  try {
    await axios.put(`/api/v1/admin/warehouse/checks/${recordForm.checkId}/record`, {
      itemId: recordForm.itemId,
      actualQty: recordForm.actualQty
    })
    ElMessage.success('盘点录入成功')
    recordVisible.value = false
    // 重新加载明细
    const { data } = await axios.get(`/api/v1/admin/warehouse/checks/${recordForm.checkId}`)
    if (data.code === 200) {
      detailItems.value = data.data.items || data.data || []
    }
  } catch (e) { ElMessage.error('操作失败') }
}

const completeCheck = async (row) => {
  await ElMessageBox.confirm(`确认完成盘点单 ${row.checkNo}？完成后将无法修改。`, '确认完成', { type: 'warning' })
  await axios.put(`/api/v1/admin/warehouse/checks/${row.id}/complete`)
  ElMessage.success('盘点已完成')
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchWarehouseOptions()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
