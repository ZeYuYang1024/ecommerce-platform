<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">权限管理</span>
        <p class="page-desc">管理系统菜单和 API 权限</p>
      </div>
      <el-button type="primary" @click="openCreate(null)">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 新增权限
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="flatPerms" v-loading="loading" row-key="id">
        <el-table-column prop="name" label="名称" min-width="200">
          <template #default="{ row }">
            <span :style="{ paddingLeft: (row._level || 0) * 24 + 'px' }">
              {{ row._level > 0 ? '└ ' : '' }}{{ row.name }}
            </span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="编码" width="200">
          <template #default="{ row }">
            <el-tag size="small" :type="row.type === 'api' ? 'warning' : row.type === 'button' ? 'success' : ''">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="type" label="类型" width="100" align="center">
          <template #default="{ row }">
            <span style="font-size:12px">{{ row.type || 'menu' }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="path" label="路径" width="180">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:11px;color:var(--text-muted)">{{ row.path || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center">
          <template #default="{ row }">
            <el-button size="small" @click="openCreate(row)">添加子级</el-button>
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="isEdit ? '编辑权限' : '新增权限'" width="520px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="权限名称" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="唯一编码" />
        </el-form-item>
        <el-form-item label="类型">
          <el-select v-model="form.type" style="width:100%">
            <el-option label="菜单" value="menu" />
            <el-option label="按钮" value="button" />
            <el-option label="API" value="api" />
          </el-select>
        </el-form-item>
        <el-form-item label="父级">
          <el-input :model-value="parentName" disabled placeholder="根权限" />
        </el-form-item>
        <el-form-item label="路径">
          <el-input v-model="form.path" placeholder="路由路径或API路径" />
        </el-form-item>
        <el-form-item label="图标">
          <el-input v-model="form.icon" placeholder="Element Plus 图标名" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" />
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
const permTree = ref([])
const flatPerms = ref([])
const visible = ref(false)

function flattenTree(nodes, level) {
  level = level || 0
  const result = []
  for (const node of nodes) {
    result.push({ ...node, _level: level, children: undefined })
    if (node.children && node.children.length > 0) {
      result.push(...flattenTree(node.children, level + 1))
    }
  }
  return result
}
const isEdit = ref(false)
const parentName = ref('')
const form = ref({ name: '', code: '', type: 'menu', parentId: null, path: '', icon: '', sort: 0 })
let editId = null

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/permissions')
    if (data.code === 200) { permTree.value = data.data || []; flatPerms.value = flattenTree(data.data || []) }
  } finally { loading.value = false }
}

function openCreate(parent) {
  isEdit.value = false; editId = null
  parentName.value = parent ? parent.name : '根权限'
  form.value = { name: '', code: '', type: 'menu', parentId: parent ? parent.id : null, path: '', icon: '', sort: 0 }
  visible.value = true
}

function openEdit(row) {
  isEdit.value = true; editId = row.id
  parentName.value = row.parentId ? '--' : '根权限'
  form.value = { name: row.name, code: row.code, type: row.type || 'menu', parentId: row.parentId, path: row.path || '', icon: row.icon || '', sort: row.sort || 0 }
  visible.value = true
}

async function save() {
  try {
    const payload = { ...form.value }
    if (isEdit.value) {
      await axios.put(`/api/v1/admin/permissions/${editId}`, payload)
    } else {
      await axios.post('/api/v1/admin/permissions', payload)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    visible.value = false
    fetchData()
  } catch { ElMessage.error('操作失败') }
}

async function doDelete(id) {
  await axios.delete(`/api/v1/admin/permissions/${id}`)
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
</style>
