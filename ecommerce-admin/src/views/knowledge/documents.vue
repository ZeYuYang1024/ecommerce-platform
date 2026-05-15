<template>
  <div class="page-container">
    <div class="page-header">
      <h2>知识库管理</h2>
      <el-button type="primary" @click="openCreate">新建文档</el-button>
    </div>

    <div class="filter-row">
      <el-select v-model="filterCategoryId" placeholder="全部分类" clearable @change="fetchData" style="width:200px">
        <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-select v-model="filterStatus" placeholder="全部状态" clearable @change="fetchData" style="width:160px; margin-left:12px">
        <el-option label="已发布" value="published" />
        <el-option label="草稿" value="draft" />
        <el-option label="已归档" value="archived" />
      </el-select>
    </div>

    <el-table :data="list" border stripe v-loading="loading" style="width:100%">
      <el-table-column prop="id" label="ID" width="180" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="分类" width="120">
        <template #default="{ row }">{{ row.categoryName || '-' }}</template>
      </el-table-column>
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)" size="small">{{ statusLabel(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="chunkCount" label="分块数" width="80" align="center" />
      <el-table-column label="更新时间" width="180">
        <template #default="{ row }">{{ row.updateTime }}</template>
      </el-table-column>
      <el-table-column label="操作" width="240" fixed="right">
        <template #default="{ row }">
          <el-button text type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button text type="warning" size="small" @click="handleReindex(row)" :loading="row._reindexing">重新索引</el-button>
          <el-popconfirm title="确定删除该文档？" @confirm="handleDelete(row)">
            <template #reference>
              <el-button text type="danger" size="small">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>

    <div class="pagination-row">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :page-sizes="[10, 20, 50]"
        :total="total"
        layout="total, sizes, prev, pager, next, jumper"
        @size-change="handleSizeChange"
        @current-change="fetchData"
      />
    </div>

    <el-dialog v-model="showDialog" :title="editing ? '编辑文档' : '新建文档'" width="640px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="标题">
          <el-input v-model="form.title" placeholder="文档标题" />
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="form.categoryId" placeholder="选择分类" style="width:100%">
            <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容">
          <el-input v-model="form.content" type="textarea" :rows="10" placeholder="文档正文内容..." />
        </el-form-item>
        <el-form-item label="来源">
          <el-select v-model="form.sourceType" style="width:100%">
            <el-option label="手动录入" value="manual" />
            <el-option label="批量导入" value="import" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="editing" label="状态">
          <el-select v-model="form.status" style="width:100%">
            <el-option label="已发布" value="published" />
            <el-option label="草稿" value="draft" />
            <el-option label="已归档" value="archived" />
          </el-select>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showDialog = false">取消</el-button>
        <el-button type="primary" @click="save" :loading="saving">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const api = axios.create({ baseURL: 'http://localhost:8095' })

const list = ref([])
const categories = ref([])
const loading = ref(false)
const saving = ref(false)
const showDialog = ref(false)
const editing = ref(false)
const page = ref(1)
const size = ref(20)
const total = ref(0)
const filterCategoryId = ref(null)
const filterStatus = ref('')
const form = ref({})

function statusLabel(s) {
  return { published: '已发布', draft: '草稿', archived: '已归档' }[s] || s
}
function statusType(s) {
  return { published: 'success', draft: 'info', archived: 'warning' }[s] || 'info'
}

function handleSizeChange() {
  page.value = 1
  fetchData()
}

async function fetchData() {
  loading.value = true
  try {
    const params = { page: page.value, size: size.value }
    if (filterCategoryId.value) params.categoryId = filterCategoryId.value
    if (filterStatus.value) params.status = filterStatus.value
    const { data } = await api.get('/api/v1/admin/knowledge/documents', { params })
    if (data.code === 0) {
      list.value = (data.data?.records || []).map(r => ({ ...r, _reindexing: false }))
      total.value = Number(data.data?.total) || list.value.length
    }
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  try {
    const { data } = await api.get('/api/v1/admin/knowledge/categories')
    if (data.code === 0) categories.value = data.data || []
  } catch { /* ignore */ }
}

function resetForm() {
  form.value = { title: '', categoryId: null, content: '', sourceType: 'manual', status: 'published' }
  editing.value = false
}

function openCreate() {
  resetForm()
  showDialog.value = true
}

function openEdit(row) {
  form.value = {
    id: row.id,
    title: row.title,
    categoryId: row.categoryId,
    content: row.content,
    sourceType: row.sourceType,
    status: row.status
  }
  editing.value = true
  showDialog.value = true
}

async function save() {
  saving.value = true
  try {
    const payload = { ...form.value }
    if (editing.value) {
      await api.put(`/api/v1/admin/knowledge/documents/${payload.id}`, payload)
    } else {
      await api.post('/api/v1/admin/knowledge/documents', payload)
    }
    showDialog.value = false
    resetForm()
    fetchData()
  } finally {
    saving.value = false
  }
}

async function handleDelete(row) {
  try {
    await api.delete(`/api/v1/admin/knowledge/documents/${row.id}`)
    fetchData()
  } catch { /* ignore */ }
}

async function handleReindex(row) {
  row._reindexing = true
  try {
    await api.post(`/api/v1/admin/knowledge/documents/${row.id}/reindex`)
    fetchData()
  } catch { /* ignore */ }
  row._reindexing = false
}

onMounted(() => {
  fetchCategories()
  fetchData()
})
</script>

<style scoped>
.page-container { padding: 24px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 20px; }
.page-header h2 { margin: 0; font-size: 20px; }
.filter-row { margin-bottom: 16px; }
.pagination-row { padding: 18px 24px; display: flex; justify-content: flex-end; border-top: 1px solid var(--border-subtle); background: var(--bg-card); }
</style>
