<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">{{ pageTitle }}</span>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right: 6px"><Plus /></el-icon>{{ createButtonText }}
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading" size="small">
        <el-table-column label="品牌" min-width="280">
          <template #default="{ row }">
            <div class="brand-cell">
              <img v-if="row.logo" :src="row.logo" class="brand-logo" />
              <span v-else class="brand-logo-placeholder">{{ row.name?.charAt(0) }}</span>
              <div class="brand-meta">
                <span class="brand-name">{{ row.name }}</span>
                <span v-if="isMerchantView" class="brand-subtitle">{{ merchantStatusLabel(row.auditStatus) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <span class="text-muted">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isMerchantView" label="更新时间" width="168">
          <template #default="{ row }">
            <span class="font-mono time-text">{{ formatDateTime(row.updatedAt || row.createdAt) }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="isMerchantView" label="审核状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="merchantStatusType(row.auditStatus)" size="small">
              {{ merchantStatusLabel(row.auditStatus) }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference>
                <el-button size="small" type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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

    <el-dialog v-model="visible" :title="dialogTitle" width="480px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="品牌名称" />
        </el-form-item>
        <el-form-item label="Logo URL">
          <el-input v-model="form.logo" placeholder="品牌 Logo 地址" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="品牌简介" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" @click="save">{{ saveButtonText }}</el-button>
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
const isMerchantView = computed(() => route.path.startsWith('/merchant/brands'))
const baseUrl = computed(() => (isMerchantView.value ? '/api/v1/admin/merchant/brands' : '/api/v1/admin/brands'))
const pageTitle = computed(() => (isMerchantView.value ? '品牌申请' : '品牌管理'))
const pageDesc = computed(() => (isMerchantView.value ? '管理自有品牌申请与品牌资料' : '管理平台与商家可用的商品品牌'))
const createButtonText = computed(() => (isMerchantView.value ? '申请品牌' : '新增品牌'))
const dialogTitle = computed(() => {
  if (isMerchantView.value) {
    return isEdit.value ? '编辑品牌申请' : '申请品牌'
  }
  return isEdit.value ? '编辑品牌' : '新增品牌'
})
const saveButtonText = computed(() => (isMerchantView.value && !isEdit.value ? '提交申请' : '保存'))
const emptyDescription = computed(() => (isMerchantView.value ? '暂无自有品牌，可先申请品牌' : '暂无品牌记录'))

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const visible = ref(false)
const isEdit = ref(false)
const form = ref({ name: '', logo: '', description: '' })
let editId = null

function merchantStatusLabel(status) {
  const map = {
    pending: '待审核',
    approved: '已通过',
    rejected: '已驳回'
  }
  return map[status] || '待审核'
}

function merchantStatusType(status) {
  const map = {
    pending: 'warning',
    approved: 'success',
    rejected: 'danger'
  }
  return map[status] || 'info'
}

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get(baseUrl.value, {
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

function openCreate() {
  isEdit.value = false
  editId = null
  form.value = { name: '', logo: '', description: '' }
  visible.value = true
}

function openEdit(row) {
  isEdit.value = true
  editId = row.id
  form.value = { name: row.name, logo: row.logo || '', description: row.description || '' }
  visible.value = true
}

async function save() {
  try {
    if (isEdit.value) {
      await axios.put(`${baseUrl.value}/${editId}`, form.value)
    } else {
      await axios.post(baseUrl.value, form.value)
    }
    ElMessage.success(isMerchantView.value && !isEdit.value ? '品牌申请已提交' : '保存成功')
    visible.value = false
    fetchData()
  } catch {
    ElMessage.error('操作失败')
  }
}

async function doDelete(id) {
  await axios.delete(`${baseUrl.value}/${id}`)
  fetchData()
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

.brand-cell {
  display: flex;
  align-items: center;
  gap: 12px;
}

.brand-meta {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand-logo {
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  object-fit: cover;
}

.brand-logo-placeholder {
  width: 36px;
  height: 36px;
  border-radius: var(--radius);
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: var(--text-muted);
}

.brand-name {
  font-weight: 600;
  font-size: 14px;
  color: var(--text-primary);
}

.brand-subtitle,
.text-muted {
  font-size: 13px;
  color: var(--text-muted);
}

.time-text {
  font-size: 12px;
  color: var(--text-secondary);
  white-space: nowrap;
}

.pagination-row {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
}

.table-card :deep(.el-table .cell) {
  line-height: 1.5;
}
</style>
