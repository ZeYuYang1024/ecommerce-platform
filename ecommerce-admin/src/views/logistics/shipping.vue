<template>
  <div class="shipping-page">
    <el-card>
      <template #header><span>发货单管理</span></template>
      <el-form :inline="true" :model="query" @submit.prevent="fetchData">
        <el-form-item label="订单号">
          <el-input v-model="query.orderNo" placeholder="订单号" clearable />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.shippingStatus" placeholder="全部" clearable>
            <el-option label="待交运" :value="0" />
            <el-option label="已交运" :value="1" />
            <el-option label="运输中" :value="2" />
            <el-option label="派送中" :value="3" />
            <el-option label="已签收" :value="4" />
            <el-option label="异常" :value="5" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="fetchData">查询</el-button>
        </el-form-item>
      </el-form>

      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="shippingNo" label="发货单号" width="180" />
        <el-table-column prop="orderNo" label="订单号" width="180" />
        <el-table-column prop="providerName" label="物流公司" width="120" />
        <el-table-column prop="trackingNo" label="运单号" width="180" />
        <el-table-column prop="shippingStatusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.shippingStatus)">{{ row.shippingStatusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastTraceDesc" label="最新轨迹" min-width="200" show-overflow-tooltip />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
        <el-table-column label="操作" width="120">
          <template #default="{ row }">
            <el-button size="small" @click="$router.push(`/logistics/shipping/${row.id}`)">详情</el-button>
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
const query = reactive({ orderNo: '', shippingStatus: null })

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/logistics/shipping', {
      params: { page: currentPage.value, size: pageSize.value, ...query }
    })
    if (data.code === 200) {
      tableData.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } finally { loading.value = false }
}

const statusTagType = (status) => {
  return [0, 1].includes(status) ? 'warning' : [2, 3].includes(status) ? 'primary' : status === 4 ? 'success' : 'danger'
}

onMounted(fetchData)
</script>
