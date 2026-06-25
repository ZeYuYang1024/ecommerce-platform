<template>
  <div class="template-page">
    <el-card>
      <template #header>
        <div class="card-header">
          <span>运费模板管理</span>
          <el-button type="primary" @click="showAddDialog">新增模板</el-button>
        </div>
      </template>
      <el-table :data="tableData" v-loading="loading" stripe>
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column prop="templateName" label="模板名称" width="150" />
        <el-table-column prop="calcTypeText" label="计费方式" width="100" />
        <el-table-column prop="firstUnit" label="首数" width="80" />
        <el-table-column prop="firstFee" label="首费(元)" width="100" />
        <el-table-column prop="continueUnit" label="续数" width="80" />
        <el-table-column prop="continueFee" label="续费(元)" width="100" />
        <el-table-column label="包邮条件" min-width="200">
          <template #default="{ row }">
            <span>{{ row.freeCondition || '无条件' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
            <el-button size="small" type="danger" @click="deleteTemplate(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
        :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
    </el-card>

    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑模板' : '新增模板'" width="600px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="模板名称" required>
          <el-input v-model="form.templateName" placeholder="如 全国统一运费" />
        </el-form-item>
        <el-form-item label="计费方式" required>
          <el-select v-model="form.calcType" style="width: 100%">
            <el-option label="按件" :value="0" />
            <el-option label="按重量" :value="1" />
            <el-option label="按体积" :value="2" />
          </el-select>
        </el-form-item>
        <el-form-item label="首数">
          <el-input-number v-model="form.firstUnit" :min="0" style="width: 200px" />
          <span class="tip">{{ calcTypeTip }}</span>
        </el-form-item>
        <el-form-item label="首费(元)">
          <el-input-number v-model="form.firstFee" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="续数">
          <el-input-number v-model="form.continueUnit" :min="0" style="width: 200px" />
          <span class="tip">{{ calcTypeTip }}</span>
        </el-form-item>
        <el-form-item label="续费(元)">
          <el-input-number v-model="form.continueFee" :min="0" :precision="2" style="width: 200px" />
        </el-form-item>
        <el-form-item label="包邮条件">
          <el-input v-model="form.freeCondition" placeholder='如 {"type":"amount","threshold":99} 表示满99包邮' type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="地区规则">
          <el-input v-model="form.regionRules" placeholder='如 {"110000":{"firstFee":10,"continueFee":5}}' type="textarea" :rows="3" />
        </el-form-item>
        <el-form-item label="所属商家" v-if="!isEdit">
          <el-input-number v-model="form.merchantId" :min="0" style="width: 200px" placeholder="商家ID" />
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
import { ref, reactive, computed, onMounted } from 'vue'
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
  id: null, templateName: '', calcType: 0, firstUnit: 1, firstFee: 0,
  continueUnit: 1, continueFee: 0, freeCondition: '', regionRules: '', merchantId: 0
})

const calcTypeTip = computed(() => {
  const map = { 0: '(件)', 1: '(克)', 2: '(cm³)' }
  return map[form.calcType] || '(件)'
})

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/logistics/templates', {
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
  Object.assign(form, {
    id: null, templateName: '', calcType: 0, firstUnit: 1, firstFee: 0,
    continueUnit: 1, continueFee: 0, freeCondition: '', regionRules: '', merchantId: 0
  })
  dialogVisible.value = true
}

const showEditDialog = (row) => {
  isEdit.value = true
  const parsedFree = typeof row.freeCondition === 'object' ? JSON.stringify(row.freeCondition) : (row.freeCondition || '')
  const parsedRegion = typeof row.regionRules === 'object' ? JSON.stringify(row.regionRules) : (row.regionRules || '')
  Object.assign(form, {
    ...row,
    freeCondition: parsedFree,
    regionRules: parsedRegion
  })
  dialogVisible.value = true
}

const submitForm = async () => {
  try {
    if (isEdit.value) {
      await axios.put(`/api/v1/admin/logistics/templates/${form.id}`, form)
    } else {
      await axios.post('/api/v1/admin/logistics/templates', form)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteTemplate = async (row) => {
  await ElMessageBox.confirm(`确定删除模板 "${row.templateName}"？`, '确认删除', { type: 'warning' })
  await axios.delete(`/api/v1/admin/logistics/templates/${row.id}`)
  ElMessage.success('已删除')
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
.tip { margin-left: 8px; font-size: 12px; color: #9CA3AF; }
</style>
