<template>
  <div class="page-container">
    <div class="page-header">
      <h2>秒杀管理</h2>
      <div>
        <el-button @click="showSessionDialog=true">新建场次</el-button>
        <el-button type="primary" @click="showItemDialog=true" :disabled="!sessions.length">添加商品</el-button>
      </div>
    </div>

    <el-tabs v-model="activeTab">
      <el-tab-pane label="秒杀场次" name="session">
        <el-table :data="sessions" border stripe>
          <el-table-column prop="id" label="ID" width="180" />
          <el-table-column prop="name" label="场次名称" />
          <el-table-column label="时间" width="320">
            <template #default="{row}">{{ row.startTime?.substring(0,16) }} ~ {{ row.endTime?.substring(0,16) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{row}">
              <el-tag :type="row.status===1?'success':row.status===2?'info':'warning'">
                {{ ['未开始','进行中','已结束'][row.status||0] }}
              </el-tag>
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
      </el-tab-pane>
      <el-tab-pane label="秒杀商品" name="item">
        <el-table :data="items" border stripe>
          <el-table-column prop="id" label="ID" width="180" />
          <el-table-column prop="name" label="商品名称" />
          <el-table-column label="原价/秒杀价" width="180">
            <template #default="{row}"><del class="text-gray-400">¥{{ row.originalPrice }}</del> <span class="text-red-500 font-bold">¥{{ row.seckillPrice }}</span></template>
          </el-table-column>
          <el-table-column label="库存" width="120">
            <template #default="{row}">{{ row.remainingCount }}/{{ row.stockCount }}</template>
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
      </el-tab-pane>
    </el-tabs>

    <!-- Session Dialog -->
    <el-dialog v-model="showSessionDialog" title="新建秒杀场次" width="460px">
      <el-form :model="sessionForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="sessionForm.name" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="sessionForm.startTime" type="datetime" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="sessionForm.endTime" type="datetime" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showSessionDialog=false">取消</el-button><el-button type="primary" @click="saveSession">保存</el-button></template>
    </el-dialog>

    <!-- Item Dialog -->
    <el-dialog v-model="showItemDialog" title="添加秒杀商品" width="460px">
      <el-form :model="itemForm" label-width="80px">
        <el-form-item label="场次"><el-select v-model="itemForm.sessionId" style="width:100%"><el-option v-for="s in sessions" :key="s.id" :label="s.name" :value="s.id" /></el-select></el-form-item>
        <el-form-item label="商品名称"><el-input v-model="itemForm.name" /></el-form-item>
        <el-form-item label="SPU ID"><el-input-number v-model="itemForm.spuId" :min="1" /></el-form-item>
        <el-form-item label="SKU ID"><el-input-number v-model="itemForm.skuId" :min="1" /></el-form-item>
        <el-form-item label="原价"><el-input-number v-model="itemForm.originalPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="秒杀价"><el-input-number v-model="itemForm.seckillPrice" :min="0" :precision="2" /></el-form-item>
        <el-form-item label="库存"><el-input-number v-model="itemForm.stockCount" :min="1" /></el-form-item>
      </el-form>
      <template #footer><el-button @click="showItemDialog=false">取消</el-button><el-button type="primary" @click="saveItem">保存</el-button></template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:5173' })
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
const sessionForm = ref({name:'',startTime:'',endTime:''})
const itemForm = ref({sessionId:null,name:'',spuId:1,skuId:1,originalPrice:0,seckillPrice:0,stockCount:100,remainingCount:100,status:1})

function handleSessionSizeChange() {
  sessionPage.value = 1
  fetchSessions()
}

function handleItemSizeChange() {
  itemPage.value = 1
  fetchItems()
}

async function fetchSessions() {
  const { data } = await api.get('/api/v1/admin/seckill/sessions', {
    params: { page: sessionPage.value, size: size.value }
  })
  if (data.code === 200) {
    sessions.value = data.data?.records || []
    sessionTotal.value = data.data?.total || 0
  }
}

async function fetchItems() {
  const { data } = await api.get('/api/v1/admin/seckill/items', {
    params: { page: itemPage.value, size: size.value }
  })
  if (data.code === 200) {
    items.value = data.data?.records || []
    itemTotal.value = data.data?.total || 0
  }
}

async function fetch() {
  await Promise.all([fetchSessions(), fetchItems()])
}

async function saveSession() {
  await api.post('/api/v1/admin/seckill/sessions', {...sessionForm.value, status:0})
  showSessionDialog.value = false
  sessionForm.value = {name:'',startTime:'',endTime:''}
  fetch()
}

async function saveItem() {
  await api.post('/api/v1/admin/seckill/items', itemForm.value)
  showItemDialog.value = false
  fetch()
}

onMounted(fetch)
</script>
<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
.text-gray-400 { color: #9ca3af; }
.text-red-500 { color: #ef4444; }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
