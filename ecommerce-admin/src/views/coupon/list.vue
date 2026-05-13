<template>
  <div class="page-container">
    <div class="page-header">
      <h2>优惠券管理</h2>
      <el-button type="primary" @click="showDialog = true">新建优惠券</el-button>
    </div>

    <el-table :data="templates" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="name" label="券名称" width="200" />
      <el-table-column label="类型" width="120">
        <template #default="{row}">{{ typeLabel(row.type) }}</template>
      </el-table-column>
      <el-table-column label="优惠" width="150">
        <template #default="{row}">
          <span v-if="row.type==='FLAT'">立减 ¥{{ row.discountAmount }}</span>
          <span v-else-if="row.type==='DISCOUNT'">{{ (row.discountRate*100).toFixed(0) }}折</span>
          <span v-else>满{{ row.minAmount }}减{{ row.discountAmount }}</span>
        </template>
      </el-table-column>
      <el-table-column prop="remainingCount" label="剩余/总量" width="120">
        <template #default="{row}">{{ row.remainingCount }}/{{ row.totalCount }}</template>
      </el-table-column>
      <el-table-column label="有效期" width="220">
        <template #default="{row}">{{ row.startTime }} ~ {{ row.endTime }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{row}">{{ row.status===1?'启用':'禁用' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="150">
        <template #default="{row}">
          <el-button text type="primary" @click="edit(row)">编辑</el-button>
          <el-button text type="danger" @click="toggleStatus(row)">{{ row.status===1?'禁用':'启用' }}</el-button>
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

    <el-dialog v-model="showDialog" :title="editing?'编辑优惠券':'新建优惠券'" width="520px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="券名称"><el-input v-model="form.name" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="满减" value="FULL_REDUCTION" />
            <el-option label="折扣" value="DISCOUNT" />
            <el-option label="立减" value="FLAT" />
          </el-select>
        </el-form-item>
        <el-form-item label="最低消费"><el-input-number v-model="form.minAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item v-if="form.type!=='DISCOUNT'" label="减免金额"><el-input-number v-model="form.discountAmount" :min="0" :precision="2" /></el-form-item>
        <el-form-item v-if="form.type==='DISCOUNT'" label="折扣率"><el-input-number v-model="form.discountRate" :min="0" :max="1" :step="0.05" :precision="2" /></el-form-item>
        <el-form-item label="发行总量"><el-input-number v-model="form.totalCount" :min="1" /></el-form-item>
        <el-form-item label="每人限领"><el-input-number v-model="form.perUserLimit" :min="1" /></el-form-item>
        <el-form-item label="开始时间"><el-date-picker v-model="form.startTime" type="datetime" style="width:100%" /></el-form-item>
        <el-form-item label="结束时间"><el-date-picker v-model="form.endTime" type="datetime" style="width:100%" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog=false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const templates = ref([])
const loading = ref(false)
const page = ref(1)
const size = ref(10)
const total = ref(0)
const showDialog = ref(false)
const editing = ref(false)
const form = ref({})

const api = axios.create({ baseURL: 'http://localhost:5173' })

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await api.get('/api/v1/admin/coupons', {
      params: { page: page.value, size: size.value }
    })
    if (data.code === 200) {
      templates.value = data.data?.records || []
      total.value = Number(data.data?.total) || templates.value.length
    }
  } finally { loading.value = false }
}

function typeLabel(t) { return {FULL_REDUCTION:'满减',DISCOUNT:'折扣',FLAT:'立减'}[t]||t }

function edit(row) { form.value = {...row}; editing.value = true; showDialog.value = true }
function resetForm() { form.value = {type:'FULL_REDUCTION',minAmount:0,discountAmount:0,discountRate:1,totalCount:100,perUserLimit:1,remainingCount:100,status:1}; editing.value = false }

async function save() {
  const payload = {...form.value}
  if (editing.value) await api.put(`/api/v1/admin/coupons/${payload.id}`, payload)
  else await api.post('/api/v1/admin/coupons', payload)
  showDialog.value = false; resetForm(); fetchData()
}

async function toggleStatus(row) {
  await api.put(`/api/v1/admin/coupons/${row.id}`, {...row, status: row.status===1?0:1})
  fetchData()
}

onMounted(() => { resetForm(); fetchData() })
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
