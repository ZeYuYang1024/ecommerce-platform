<template>
  <div class="stock-page">
    <el-card>
      <template #header><span>库存查询</span></template>
      <el-form :inline="true" :model="query" @submit.prevent="fetchData">
        <el-form-item label="仓库">
          <el-select v-model="query.warehouseId" placeholder="全部" clearable>
            <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="SKU ID">
          <el-input v-model="query.skuId" placeholder="SKU ID" clearable />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe :row-class-name="tableRowClassName">
        <el-table-column prop="warehouseId" label="仓库ID" width="100" />
        <el-table-column prop="warehouseName" label="仓库名称" width="150" />
        <el-table-column prop="skuId" label="SKU ID" width="100" />
        <el-table-column prop="skuName" label="SKU名称" width="150" />
        <el-table-column prop="binId" label="库位ID" width="100" />
        <el-table-column prop="binName" label="库位名称" width="120" />
        <el-table-column prop="quantity" label="库存数量" width="100" />
        <el-table-column prop="lockedQty" label="锁定数量" width="100" />
        <el-table-column prop="availableQty" label="可用数量" width="100" />
        <el-table-column prop="safetyStock" label="安全库存" width="100" />
        <el-table-column label="库存状态" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.availableQty <= row.safetyStock" type="danger">库存不足</el-tag>
            <el-tag v-else type="success">库存正常</el-tag>
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

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const warehouseOptions = ref([])
const query = reactive({ warehouseId: null, skuId: '' })

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/warehouse/stock', {
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

const tableRowClassName = ({ row }) => {
  if (row.availableQty != null && row.safetyStock != null && row.availableQty <= row.safetyStock) {
    return 'low-stock-row'
  }
  return ''
}

onMounted(() => {
  fetchData()
  fetchWarehouseOptions()
})
</script>

<style scoped>
:deep(.low-stock-row) {
  background-color: #fef0f0 !important;
}
</style>
