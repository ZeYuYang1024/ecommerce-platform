<template>
  <div class="outbound-page">
    <el-card>
      <template #header><span>出库单管理</span></template>
      <el-form :inline="true" :model="query" @submit.prevent="fetchData">
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable>
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" placeholder="全部" clearable>
            <el-option label="待拣货" :value="0" />
            <el-option label="拣货中" :value="1" />
            <el-option label="已发货" :value="2" />
            <el-option label="已取消" :value="3" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="outboundNo" label="出库单号" width="180" />
        <el-table-column prop="warehouseName" label="仓库" width="150" />
        <el-table-column prop="outboundTypeText" label="出库类型" width="100" />
        <el-table-column prop="shippingId" label="发货单ID" width="100" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="outboundStatusTag(row.status)">{{ outboundStatusText(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="200" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 0" size="small" type="warning" @click="startPick(row)">开始拣货</el-button>
            <el-button v-if="row.status === 1" size="small" type="success" @click="confirmShip(row)">确认发货</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>
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

const outboundStatusText = (status) => {
  return ['待拣货', '拣货中', '已发货', '已取消'][status] || '未知'
}

const outboundStatusTag = (status) => {
  return status === 0 ? 'warning' : status === 1 ? 'primary' : status === 2 ? 'success' : 'info'
}

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/warehouse/outbounds', {
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

const startPick = async (row) => {
  await ElMessageBox.confirm(`确认开始拣货出库单 ${row.outboundNo}？`, '开始拣货', { type: 'warning' })
  await axios.put(`/api/v1/admin/warehouse/outbounds/${row.id}/pick`)
  ElMessage.success('已开始拣货')
  fetchData()
}

const confirmShip = async (row) => {
  await ElMessageBox.confirm(`确认出库单 ${row.outboundNo} 已发货？`, '确认发货', { type: 'warning' })
  await axios.put(`/api/v1/admin/warehouse/outbounds/${row.id}/ship`)
  ElMessage.success('已确认发货')
  fetchData()
}

onMounted(() => {
  fetchData()
  fetchWarehouseOptions()
})
</script>
