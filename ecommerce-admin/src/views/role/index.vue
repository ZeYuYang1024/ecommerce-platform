<template>
  <div>
    <div class="toolbar">
      <div>
        <span class="section-label">角色管理</span>
        <p class="page-desc">管理系统角色，分配权限</p>
      </div>
      <el-button type="primary" @click="openCreate">
        <el-icon style="margin-right:6px"><Plus /></el-icon> 新增角色
      </el-button>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="roles" v-loading="loading">
        <el-table-column prop="name" label="角色名称" min-width="150">
          <template #default="{ row }">
            <span style="font-weight:600">{{ row.name }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="code" label="角色编码" width="160">
          <template #default="{ row }">
            <el-tag size="small" type="info">{{ row.code }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="description" label="描述" min-width="200">
          <template #default="{ row }">
            <span style="font-size:13px;color:var(--text-muted)">{{ row.description || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center">
          <template #default="{ row }">
            <el-button size="small" type="primary" @click="openEdit(row)">编辑</el-button>
            <el-popconfirm title="确认删除？" @confirm="doDelete(row.id)">
              <template #reference><el-button size="small" type="danger">删除</el-button></template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="visible" :title="isEdit ? '编辑角色' : '新增角色'" width="560px">
      <el-form :model="form" label-width="80px">
        <el-form-item label="名称">
          <el-input v-model="form.name" placeholder="角色名称" />
        </el-form-item>
        <el-form-item label="编码">
          <el-input v-model="form.code" placeholder="唯一编码，如 admin" />
        </el-form-item>
        <el-form-item label="描述">
          <el-input v-model="form.description" type="textarea" :rows="2" placeholder="角色说明" />
        </el-form-item>
        <el-form-item label="权限分配">
          <el-tree
            ref="permTree"
            :data="permTree"
            :props="{ children: 'children', label: 'name' }"
            node-key="id"
            show-checkbox
            :default-checked-keys="form.permissionIds || []"
            style="max-height:300px;overflow:auto"
          />
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
const roles = ref([])
const permTree = ref([])
const visible = ref(false)
const isEdit = ref(false)
const permTreeRef = ref(null)
const form = ref({ name: '', code: '', description: '', permissionIds: [] })
let editId = null

async function fetchRoles() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/roles')
    if (data.code === 200) roles.value = data.data || []
  } finally { loading.value = false }
}

async function fetchPerms() {
  const { data } = await axios.get('/api/v1/admin/permissions')
  if (data.code === 200) permTree.value = data.data || []
}

function openCreate() {
  isEdit.value = false; editId = null
  form.value = { name: '', code: '', description: '', permissionIds: [] }
  visible.value = true
}

function openEdit(row) {
  isEdit.value = true; editId = row.id
  form.value = { name: row.name, code: row.code, description: row.description || '', permissionIds: row.permissionIds || [] }
  visible.value = true
}

async function save() {
  try {
    const checked = permTreeRef.value?.getCheckedKeys() || []
    const payload = { ...form.value, permissionIds: checked }
    if (isEdit.value) {
      await axios.put(`/api/v1/admin/roles/${editId}`, payload)
    } else {
      await axios.post('/api/v1/admin/roles', payload)
    }
    ElMessage.success(isEdit.value ? '更新成功' : '创建成功')
    visible.value = false
    fetchRoles()
  } catch { ElMessage.error('操作失败') }
}

async function doDelete(id) {
  await axios.delete(`/api/v1/admin/roles/${id}`)
  fetchRoles()
}

onMounted(() => { fetchRoles(); fetchPerms() })
</script>

<style scoped>
.toolbar { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 20px; }
.page-desc { font-size: 13.5px; color: var(--text-muted); margin-top: 4px; }
.table-card { border-radius: var(--radius-lg); }
</style>
