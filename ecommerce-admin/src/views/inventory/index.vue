<template>
  <div>
    <div class="page-header">
      <h1 class="section-label">库存管理</h1>
      <p class="page-desc">{{ pageDesc }}</p>
    </div>

    <div class="toolbar">
      <div class="toolbar-left">
        <el-input v-model="skuIdFilter" placeholder="SKU ID" style="width:200px" size="large" clearable @keyup.enter="search" />
        <el-select v-model="stockStatus" placeholder="库存状态" style="width:160px" size="large" clearable @change="search">
          <el-option label="缺货" :value="0" />
          <el-option label="低库存" :value="1" />
          <el-option label="有货" :value="2" />
        </el-select>
      </div>
      <div class="toolbar-right">
        <el-button size="large" @click="search" :loading="loading">
          <el-icon style="margin-right:6px"><Search /></el-icon> 查询
        </el-button>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="SKU ID" width="200">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.skuId }}</span>
          </template>
        </el-table-column>
        <el-table-column label="SKU 名称" min-width="160">
          <template #default="{ row }">
            <span v-if="row.skuName">{{ row.skuName }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="所属商品" min-width="160">
          <template #default="{ row }">
            <span v-if="row.spuName">{{ row.spuName }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="价格" width="120" align="center">
          <template #default="{ row }">
            <span v-if="row.price !== null && row.price !== undefined" class="font-mono">¥{{ row.price }}</span>
            <span v-else class="text-muted">--</span>
          </template>
        </el-table-column>
        <el-table-column label="总库存" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono stock-num">{{ row.totalStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="已锁定" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono stock-num locked">{{ row.lockedStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="可售" width="100" align="center">
          <template #default="{ row }">
            <span class="font-mono stock-num" :class="{ available: row.availableStock > 0, zero: row.availableStock === 0 }">{{ row.availableStock }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="120" align="center" fixed="right">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openUpdate(row)">更新库存</el-button>
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

    <el-dialog v-model="updateVisible" title="更新库存" width="440px">
      <div class="update-info">
        <div class="update-row"><span class="update-label">SKU ID</span><span class="font-mono">{{ updateForm.skuId }}</span></div>
        <div class="update-row" v-if="updateForm.skuName"><span class="update-label">SKU 名称</span><span>{{ updateForm.skuName }}</span></div>
        <div class="update-row"><span class="update-label">当前总库存</span><span class="font-mono">{{ updateForm.totalStock }}</span></div>
      </div>
      <div style="margin-top:20px">
        <label class="update-label" style="margin-bottom:8px;display:block">新总库存</label>
        <el-input-number v-model="newStock" :min="0" size="large" :precision="0" style="width:100%" />
      </div>
      <template #footer>
        <el-button @click="updateVisible = false">取消</el-button>
        <el-button type="primary" @click="doUpdate" :loading="saving">确认更新</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const isMerchantView = computed(() => route.path.startsWith('/merchant/inventory'))
const pageDesc = computed(() => (isMerchantView.value
  ? '查询并维护本店商品 SKU 库存'
  : '查询和管理全量商品 SKU 库存'))
const listUrl = computed(() => isMerchantView.value
  ? '/api/v1/admin/merchant/inventory'
  : '/api/v1/admin/inventory')
const updateUrl = computed(() => isMerchantView.value
  ? '/api/v1/admin/merchant/inventory'
  : '/api/v1/admin/inventory')
const emptyDescription = computed(() => (isMerchantView.value
  ? '暂无本店库存记录，可先创建商品或补充 SKU'
  : '暂无库存记录'))

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const skuIdFilter = ref('')
const stockStatus = ref(null)

const updateVisible = ref(false)
const updateForm = ref({})
const newStock = ref(0)
const saving = ref(false)

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    const sid = skuIdFilter.value.trim()
    if (sid) params.skuId = sid
    if (stockStatus.value !== null && stockStatus.value !== '') params.stockStatus = stockStatus.value
    const { data } = await axios.get(listUrl.value, { params })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = Number(data.data?.total) || tableData.value.length
    }
  } finally { loading.value = false }
}

function search() {
  page.value = 1
  updateQuery()
  fetchData()
}

function openUpdate(row) {
  updateForm.value = { ...row }
  newStock.value = row.totalStock
  updateVisible.value = true
}

async function doUpdate() {
  saving.value = true
  try {
    await axios.post(`${updateUrl.value}/${updateForm.value.skuId}`, { totalStock: newStock.value })
    ElMessage.success('库存更新成功')
    updateVisible.value = false
    fetchData()
  } catch {
    ElMessage.error('更新失败')
  } finally { saving.value = false }
}

function updateQuery() {
  const query = {}
  if (page.value !== 1) query.page = page.value
  if (size.value !== 10) query.size = size.value
  if (stockStatus.value !== null && stockStatus.value !== '') query.stockStatus = stockStatus.value
  if (skuIdFilter.value.trim()) query.skuId = skuIdFilter.value.trim()
  router.replace({ query })
}

onMounted(() => {
  const sid = route.query.skuId
  if (sid) skuIdFilter.value = sid
  const ss = route.query.stockStatus
  if (ss !== undefined && ss !== '') stockStatus.value = Number(ss)
  if (route.query.page) page.value = Number(route.query.page)
  if (route.query.size) size.value = Number(route.query.size)
  fetchData()
})
</script>

<style scoped>
.page-header {
  margin-bottom: 24px;
}
.page-desc {
  font-size: 14px;
  color: var(--text-muted);
  margin-top: 4px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.toolbar-left { display: flex; gap: 12px; }
.table-card { border-radius: var(--radius-lg); }

.id-text { font-size: 12px; color: var(--text-muted); }
.text-muted { color: var(--text-muted); }
.stock-num { font-weight: 700; }
.stock-num.locked { color: var(--accent); }
.stock-num.available { color: var(--green); }
.stock-num.zero { color: var(--text-muted); }

.update-info {
  background: var(--bg-surface);
  border-radius: var(--radius);
  padding: 16px 20px;
}
.update-row {
  display: flex;
  padding: 6px 0;
}
.update-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  width: 90px;
  flex-shrink: 0;
}

.pagination-row {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
}
</style>
