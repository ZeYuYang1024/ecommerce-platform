<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2 class="section-label">秒杀管理</h2>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <div class="header-actions">
        <el-button @click="showSessionDialog = true">新建场次</el-button>
        <el-button type="primary" @click="showItemDialog = true" :disabled="!sessions.length">添加商品</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="秒杀场次" name="session">
        <el-card shadow="never" class="table-card">
          <el-table :data="sessions" border stripe v-loading="loadingSessions">
            <el-table-column prop="id" label="ID" width="180" />
            <el-table-column prop="name" label="场次名称" min-width="180" />
            <el-table-column label="时间" width="320">
              <template #default="{ row }">
                {{ formatDateTime(row.startTime) }} ~ {{ formatDateTime(row.endTime) }}
              </template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">
                <el-tag :type="sessionStatusType(row.status)">{{ sessionStatusLabel(row.status) }}</el-tag>
              </template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="sessionPage"
              v-model:page-size="size"
              :page-sizes="[10, 20, 50, 100]"
              :total="sessionTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleSessionSizeChange"
              @current-change="fetchSessions"
            />
          </div>
        </el-card>
      </el-tab-pane>

      <el-tab-pane label="秒杀商品" name="item">
        <el-card shadow="never" class="table-card">
          <el-table :data="items" border stripe v-loading="loadingItems">
            <el-table-column prop="id" label="ID" width="180" />
            <el-table-column prop="name" label="商品名称" min-width="180" />
            <el-table-column label="原价/秒杀价" width="180">
              <template #default="{ row }">
                <del class="text-muted">{{ row.originalPrice }}</del>
                <span class="price-text">{{ row.seckillPrice }}</span>
              </template>
            </el-table-column>
            <el-table-column label="库存" width="120">
              <template #default="{ row }">{{ row.remainingCount }}/{{ row.stockCount }}</template>
            </el-table-column>
            <el-table-column label="状态" width="100">
              <template #default="{ row }">{{ row.status === 1 ? '启用' : '禁用' }}</template>
            </el-table-column>
          </el-table>

          <div class="pagination-row">
            <el-pagination
              v-model:current-page="itemPage"
              v-model:page-size="size"
              :page-sizes="[10, 20, 50, 100]"
              :total="itemTotal"
              layout="total, sizes, prev, pager, next, jumper"
              @size-change="handleItemSizeChange"
              @current-change="fetchItems"
            />
          </div>
        </el-card>
      </el-tab-pane>
    </el-tabs>

    <el-dialog v-model="showSessionDialog" title="新建秒杀场次" width="520px">
      <el-form :model="sessionForm" label-width="90px">
        <el-form-item label="场次名称">
          <el-input v-model="sessionForm.name" />
        </el-form-item>
        <el-form-item label="开始时间">
          <el-date-picker v-model="sessionForm.startTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
        <el-form-item label="结束时间">
          <el-date-picker v-model="sessionForm.endTime" type="datetime" value-format="YYYY-MM-DD HH:mm:ss" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showSessionDialog = false">取消</el-button>
        <el-button type="primary" @click="saveSession">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showItemDialog" title="添加秒杀商品" width="520px">
      <el-form :model="itemForm" label-width="90px">
        <el-form-item label="场次">
          <el-select v-model="itemForm.sessionId" style="width: 100%">
            <el-option v-for="session in sessions" :key="session.id" :label="session.name" :value="session.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="商品名称">
          <el-input v-model="itemForm.name" />
        </el-form-item>
        <el-form-item label="SPU ID">
          <el-input-number v-model="itemForm.spuId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="SKU ID">
          <el-input-number v-model="itemForm.skuId" :min="1" style="width: 100%" />
        </el-form-item>
        <el-form-item label="原价">
          <el-input-number v-model="itemForm.originalPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="秒杀价">
          <el-input-number v-model="itemForm.seckillPrice" :min="0" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="库存">
          <el-input-number v-model="itemForm.stockCount" :min="1" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showItemDialog = false">取消</el-button>
        <el-button type="primary" @click="saveItem">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { formatDateTime } from '@/utils/dateTime'

const route = useRoute()
const isMerchantView = computed(() => route.path.startsWith('/merchant/seckill'))
const pageDesc = computed(() => (isMerchantView.value
  ? '管理当前商家的秒杀场次和商品'
  : '管理平台统一秒杀活动'))
const sessionUrl = computed(() => (isMerchantView.value ? '/api/v1/admin/merchant/seckill/sessions' : '/api/v1/admin/seckill/sessions'))
const itemUrl = computed(() => (isMerchantView.value ? '/api/v1/admin/merchant/seckill/items' : '/api/v1/admin/seckill/items'))

const sessions = ref([])
const items = ref([])
const sessionPage = ref(1)
const itemPage = ref(1)
const size = ref(10)
const sessionTotal = ref(0)
const itemTotal = ref(0)
const activeTab = ref('session')
const showSessionDialog = ref(false)
const showItemDialog = ref(false)
const loadingSessions = ref(false)
const loadingItems = ref(false)
const sessionForm = ref({})
const itemForm = ref({})

function defaultSessionForm() {
  return { name: '', startTime: '', endTime: '' }
}

function defaultItemForm() {
  return {
    sessionId: null,
    name: '',
    spuId: 1,
    skuId: 1,
    originalPrice: 0,
    seckillPrice: 0,
    stockCount: 100,
    remainingCount: 100,
    status: 1
  }
}

function resetForms() {
  sessionForm.value = defaultSessionForm()
  itemForm.value = defaultItemForm()
}

function sessionStatusLabel(status) {
  return {
    0: '未开始',
    1: '进行中',
    2: '已结束'
  }[status] || '未知'
}

function sessionStatusType(status) {
  return {
    0: 'warning',
    1: 'success',
    2: 'info'
  }[status] || 'info'
}

function handleSessionSizeChange() {
  sessionPage.value = 1
  fetchSessions()
}

function handleItemSizeChange() {
  itemPage.value = 1
  fetchItems()
}

async function fetchSessions() {
  loadingSessions.value = true
  try {
    const { data } = await axios.get(sessionUrl.value, {
      params: { page: sessionPage.value, size: size.value }
    })
    if (data.code === 200) {
      sessions.value = data.data?.records || []
      sessionTotal.value = Number(data.data?.total) || sessions.value.length
    }
  } finally {
    loadingSessions.value = false
  }
}

async function fetchItems() {
  loadingItems.value = true
  try {
    const { data } = await axios.get(itemUrl.value, {
      params: { page: itemPage.value, size: size.value }
    })
    if (data.code === 200) {
      items.value = data.data?.records || []
      itemTotal.value = Number(data.data?.total) || items.value.length
    }
  } finally {
    loadingItems.value = false
  }
}

async function fetchAll() {
  await Promise.all([fetchSessions(), fetchItems()])
}

async function saveSession() {
  await axios.post(sessionUrl.value, {
    ...sessionForm.value,
    status: 0
  })
  ElMessage.success('场次已保存')
  showSessionDialog.value = false
  sessionForm.value = defaultSessionForm()
  fetchAll()
}

async function saveItem() {
  await axios.post(itemUrl.value, {
    ...itemForm.value,
    remainingCount: itemForm.value.stockCount
  })
  ElMessage.success('秒杀商品已保存')
  showItemDialog.value = false
  itemForm.value = defaultItemForm()
  fetchAll()
}

onMounted(() => {
  resetForms()
  fetchAll()
})
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}
.page-desc {
  margin-top: 4px;
  font-size: 13.5px;
  color: var(--text-muted);
}
.header-actions {
  display: flex;
  gap: 12px;
}
.table-card { border-radius: var(--radius-lg); }
.pagination-row {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
}
.text-muted { color: var(--text-muted); }
.price-text {
  margin-left: 8px;
  font-weight: 700;
  color: var(--red);
}
</style>
