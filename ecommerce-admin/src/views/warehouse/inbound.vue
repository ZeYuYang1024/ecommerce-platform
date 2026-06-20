<template>
  <div class="inbound-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>入库单管理</span>
          <el-button type="primary" @click="showCreateDialog">创建入库单</el-button>
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
            <el-option label="待收货" :value="0" />
            <el-option label="已收货" :value="1" />
            <el-option label="已上架" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="inboundNo" label="入库单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="150" />
        <el-table-column prop="inboundTypeText" label="入库类型" width="100" />
        <el-table-column prop="sourceOrderNo" label="来源单号" width="180" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="inboundStatusTag(row.status)">{{ inboundStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="primary" @click="confirmReceive(row)">确认收货</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" @click="confirmShelve(row)">确认上架</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>

    <!-- 创建入库单弹窗 -->
    <el-dialog v-model="dialogVisible" title="创建入库单" width="700px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="仓库" required>
          <el-select v-model="form.warehouseId" placeholder="请选择仓库" style="width: 100%">
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="入库类型" required>
          <el-select v-model="form.inboundType" placeholder="请选择">
            <el-option label="采购入库" :value="1" />
            <el-option label="退货入库" :value="2" />
            <el-option label="调拨入库" :value="3" />
            <el-option label="其他入库" :value="4" />
          </el-select>
        </el-form-item>
        <el-form-item label="来源单号">
          <el-input v-model="form.sourceOrderNo" placeholder="采购单号/退货单号等" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.remark" type="textarea" placeholder="备注信息" />
        </el-form-item>
        <el-divider>入库明细</el-divider>
        <div v-for="(item, index) in form.items" :key="index" style="display: flex; gap: 10px; margin-bottom: 10px; align-items: center;">
          <el-input v-model="item.skuId" placeholder="SKU ID" style="flex: 1" />
          <el-input-number v-model="item.quantity" :min="1" placeholder="数量" />
          <el-button type="danger" circle @click="removeItem(index)" :disabled="form.items.length <= 1">
            <span>&times;</span>
          </el-button>
        </div>
        <el-button type="primary" size="small" @click="addItem">+ 添加明细</el-button>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认创建</el-button>
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
const dialogVisible = ref(false)
const warehouseOptions = ref([])
const query = reactive({ warehouseId: null, status: null })
const form = reactive({
  warehouseId: null, inboundType: 1, sourceOrderNo: '', remark: '',
  items: [{ skuId: '', quantity: 1 }]
})

const inboundStatusText = (status) => {
  return ['待收货', '已收货', '已上架', '已取消'][status] || '未知'
}

const inboundStatusTag = (status) => {
  return status === 0 ? 'warning' : status === 1 ? 'primary' : status === 2 ? 'success' : 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/warehouse/inbounds', {
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
  Object.assign(form, { warehouseId: null, inboundType: 1, sourceOrderNo: '', remark: '', items: [{ skuId: '', quantity: 1 }] })
  dialogVisible.value = true
}

const addItem = () => {
  form.items.push({ skuId: '', quantity: 1 })
}

const removeItem = (index) => {
  form.items.splice(index, 1)
}

const submitForm = async () => {
  try {
    await axios.post('/api/v1/admin/warehouse/inbounds', form)
    ElMessage.success('创建成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error('操作失败') }
}

const confirmReceive = async (row) => {
  await ElMessageBox.confirm(`确认已收到入库单 ${row.inboundNo} 的货物？`, '确认收货', { type: 'warning' })
  await axios.put(`/api/v1/admin/warehouse/inbounds/${row.id}/receive`)
  ElMessage.success('已确认收货')
  fetchData()
}

const confirmShelve = async (row) => {
  await ElMessageBox.confirm(`确认入库单 ${row.inboundNo} 已全部上架？`, '确认上架', { type: 'warning' })
  await axios.put(`/api/v1/admin/warehouse/inbounds/${row.id}/shelve`)
  ElMessage.success('已确认上架')
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
