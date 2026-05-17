<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">对账管理</span>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <el-button v-if="!isMerchantView" type="primary" size="large" @click="runRecon" :loading="running">
        <el-icon style="margin-right: 6px"><RefreshRight /></el-icon> 执行对账
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="批次号" min-width="200">
          <template #default="{ row }">
            <span class="font-mono" style="font-size: 12px; color: var(--text-secondary)">{{ row.batchNo }}</span>
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
            <span class="stat-num" style="color: var(--green)">{{ row.matchedCount }}</span>
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
            <el-button size="small" @click="openDetail(row.id)">查看明细</el-button>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty :description="emptyDescription" />
        </template>
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
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const loading = ref(false)
const running = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)

const isMerchantView = computed(() => route.path.startsWith('/merchant/reconciliation'))
const listUrl = computed(() => (isMerchantView.value ? '/api/v1/admin/merchant/reconciliation' : '/api/v1/admin/reconciliation'))
const detailBasePath = computed(() => (isMerchantView.value ? '/merchant/reconciliation' : '/reconciliation'))
const pageDesc = computed(() => (isMerchantView.value ? '查看本店订单关联的对账结果和差异明细' : '比对支付记录与订单记录，发现差异'))
const emptyDescription = computed(() => (isMerchantView.value ? '暂无本店对账结果' : '暂无对账结果'))

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get(listUrl.value, {
      params: { page: page.value, size: size.value }
    })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = Number(data.data?.total) || tableData.value.length
    }
  } finally {
    loading.value = false
  }
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
  } finally {
    running.value = false
  }
}

function openDetail(id) {
  router.push(`${detailBasePath.value}/${id}`)
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

.page-desc {
  font-size: 13.5px;
  color: var(--text-muted);
  margin-top: 4px;
}

.table-card {
  border-radius: var(--radius-lg);
}

.stat-num {
  font-weight: 700;
  font-size: 15px;
  font-family: var(--font-mono);
}

.stat-num.bad {
  color: var(--red);
}

.time-text {
  font-size: 12px;
  color: var(--text-secondary);
}

.pagination-row {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
}
</style>
