<template>
  <div class="providers-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>物流公司管理</span>
          <el-button type="primary" @click="showAddDialog">新增物流公司</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="providerCode" label="编码" width="100" />
        <el-table-column prop="providerName" label="名称" width="150" />
        <el-table-column prop="priority" label="优先级" width="80" />
        <el-table-column prop="statusText" label="状态" width="80">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="250">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'"
              @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
            <el-button size="small" type="danger" @click="deleteProvider(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑物流公司' : '新增物流公司'" width="500px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="编码" required>
          <el-input v-model="form.providerCode" :disabled="isEdit" placeholder="如 SF, ZTO" />
        </el-form-item>
        <el-form-item label="名称" required>
          <el-input v-model="form.providerName" placeholder="如 顺丰速运" />
        </el-form-item>
        <el-form-item label="LOGO URL">
          <el-input v-model="form.providerLogo" placeholder="LOGO图片地址" />
        </el-form-item>
        <el-form-item label="月结账号">
          <el-input v-model="form.customerAccount" placeholder="月结账号/客户号" />
        </el-form-item>
        <el-form-item label="电子面单">
          <el-switch v-model="form.supportWaybill" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-input-number v-model="form.priority" :min="0" :max="999" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="submitForm">确认</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const form = reactive({
  id: null, providerCode: '', providerName: '', providerLogo: '',
  customerAccount: '', supportWaybill: 0, priority: 99
})

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/logistics/providers', {
      params: { page: currentPage.value, size: pageSize.value }
    })
    if (data.code === 200) {
      tableData.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } finally { loading.value = false }
}

const showAddDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, providerCode: '', providerName: '', providerLogo: '', customerAccount: '', supportWaybill: 0, priority: 99 })
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  Object.assign(form, { ...row })
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (isEdit.value) {
      await axios.put(`/api/v1/admin/logistics/providers/${form.id}`, form)
    } else {
      await axios.post('/api/v1/admin/logistics/providers', form)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error('操作失败') }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/v1/admin/logistics/providers/${row.id}/status`, null, { params: { status: newStatus } })
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  fetchData()
}

const deleteProvider = async (row) => {
  await ElMessageBox.confirm(`确定删除 ${row.providerName}？`, '确认删除', { type: 'warning' })
  await axios.delete(`/api/v1/admin/logistics/providers/${row.id}`)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
