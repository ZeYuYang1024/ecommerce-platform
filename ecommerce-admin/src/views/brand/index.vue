<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">品牌管理</span>
        <p class="page-desc">管理商品品牌</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 新增品牌
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column label="品牌" min-width="240">
          <template #default="{ row }">
            <div class="brand-cell">
              <img v-if="row.logo" :src="row.logo" class="brand-logo" />
              <span v-else class="brand-logo-placeholder">{{ row.name?.charAt(0) }}</span>
              <span class="brand-name">{{ row.name }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span style="font-size:13px;color:var(--text-muted)">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="isEdit ? '编辑品牌' : '新增品牌'" width="480px">
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
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const visible = ref(false)
const isEdit = ref(false)
const form = ref({ name: '', logo: '', description: '' })
let editId = null

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/brands')
    if (data.code === 200) tableData.value = data.data || []
  } finally { loading.value = false }
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
      await axios.put(`/api/v1/admin/brands/${editId}`, form.value)
    } else {
      await axios.post('/api/v1/admin/brands', form.value)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    visible.value = false
    fetchData()
  } catch { ElMessage.error('操作失败') }
}

async function doDelete(id) {
  await axios.delete(`/api/v1/admin/brands/${id}`)
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
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
.brand-cell { display: flex; align-items: center; gap: 12px; }
.brand-logo { width: 36px; height: 36px; border-radius: var(--radius); object-fit: cover; }
.brand-logo-placeholder {
  width: 36px; height: 36px; border-radius: var(--radius);
  background: var(--bg-surface); display: flex; align-items: center;
  justify-content: center; font-weight: 700; color: var(--text-muted);
}
.brand-name { font-weight: 600; font-size: 14px; color: var(--text-primary); }
</style>
