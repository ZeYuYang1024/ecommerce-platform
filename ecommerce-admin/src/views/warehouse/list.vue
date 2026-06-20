<template>
  <div class="warehouse-page">
    <el-tabs v-model="activeTab" type="border-card">
      <!-- 仓库管理 Tab -->
      <el-tab-pane label="仓库管理" name="warehouse">
        <el-card>
          <template #header>
            <div class="card-header">
              <span>仓库列表</span>
              <el-button type="primary" @click="showAddDialog">新增仓库</el-button>
            </div>
          </template>
          <el-table :data="tableData" v-loading="loading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="warehouseName" label="仓库名称" width="150" />
            <el-table-column prop="warehouseCode" label="仓库编码" width="120" />
            <el-table-column prop="warehouseTypeText" label="仓库类型" width="100">
              <template #default="{ row }">
                <el-tag :type="row.warehouseType === 1 ? 'primary' : 'warning'">{{ row.warehouseTypeText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="stockModeText" label="库存模式" width="100">
              <template #default="{ row }">
                <el-tag :type="row.stockMode === 1 ? 'success' : 'info'">{{ row.stockModeText }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column prop="merchantId" label="商家ID" width="100" />
            <el-table-column prop="contactName" label="联系人" width="100" />
            <el-table-column prop="contactPhone" label="联系电话" width="130" />
            <el-table-column label="状态" width="80">
              <template #default="{ row }">
                <el-tag :type="row.status === 1 ? 'success' : 'info'">{{ row.status === 1 ? '启用' : '停用' }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="260" fixed="right">
              <template #default="{ row }">
                <el-button size="small" @click="showEditDialog(row)">编辑</el-button>
                <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'"
                  @click="toggleStatus(row)">{{ row.status === 1 ? '停用' : '启用' }}</el-button>
                <el-button size="small" type="danger" @click="deleteWarehouse(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-pagination v-if="total > pageSize" v-model:current-page="currentPage" :page-size="pageSize"
            :total="total" layout="total, prev, pager, next" @current-change="fetchData" />
        </el-card>

        <!-- 仓库新增/编辑弹窗 -->
        <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑仓库' : '新增仓库'" width="600px">
          <el-form :model="form" label-width="100px">
            <el-form-item label="仓库名称" required>
              <el-input v-model="form.warehouseName" placeholder="如 上海中心仓" />
            </el-form-item>
            <el-form-item label="仓库编码" required>
              <el-input v-model="form.warehouseCode" :disabled="isEdit" placeholder="如 WH-SH-001" />
            </el-form-item>
            <el-form-item label="仓库类型" required>
              <el-select v-model="form.warehouseType" placeholder="请选择">
                <el-option label="平台仓" :value="1" />
                <el-option label="商家仓" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="库存模式" required>
              <el-select v-model="form.stockMode" placeholder="请选择">
                <el-option label="轻仓" :value="1" />
                <el-option label="托管" :value="2" />
              </el-select>
            </el-form-item>
            <el-form-item label="所属商家">
              <el-input v-model="form.merchantId" placeholder="商家ID" />
            </el-form-item>
            <el-form-item label="联系人">
              <el-input v-model="form.contactName" placeholder="联系人姓名" />
            </el-form-item>
            <el-form-item label="联系电话">
              <el-input v-model="form.contactPhone" placeholder="联系电话" />
            </el-form-item>
            <el-form-item label="地址">
              <el-input v-model="form.address" placeholder="仓库详细地址" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="dialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitForm">确认</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 库区管理 Tab -->
      <el-tab-pane label="库区管理" name="zone">
        <el-card>
          <template #header>
            <div class="card-header">
              <div>
                <el-select v-model="zoneWarehouseId" placeholder="选择仓库" @change="fetchZones" style="width: 200px">
                  <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
                </el-select>
              </div>
              <el-button type="primary" @click="showZoneAddDialog" :disabled="!zoneWarehouseId">新增库区</el-button>
            </div>
          </template>
          <el-table :data="zoneTableData" v-loading="zoneLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="zoneName" label="库区名称" width="150" />
            <el-table-column prop="zoneCode" label="库区编码" width="120" />
            <el-table-column prop="zoneTypeText" label="库区类型" width="100" />
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="showZoneEditDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteZone(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 库区新增/编辑弹窗 -->
        <el-dialog v-model="zoneDialogVisible" :title="zoneIsEdit ? '编辑库区' : '新增库区'" width="500px">
          <el-form :model="zoneForm" label-width="100px">
            <el-form-item label="库区名称" required>
              <el-input v-model="zoneForm.zoneName" placeholder="如 A区" />
            </el-form-item>
            <el-form-item label="库区编码" required>
              <el-input v-model="zoneForm.zoneCode" :disabled="zoneIsEdit" placeholder="如 Z-A-001" />
            </el-form-item>
            <el-form-item label="库区类型">
              <el-select v-model="zoneForm.zoneType" placeholder="请选择">
                <el-option label="存储区" :value="1" />
                <el-option label="拣选区" :value="2" />
                <el-option label="退货区" :value="3" />
                <el-option label="暂存区" :value="4" />
              </el-select>
            </el-form-item>
            <el-form-item label="容量">
              <el-input-number v-model="zoneForm.capacity" :min="0" :max="999999" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="zoneDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitZoneForm">确认</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>

      <!-- 库位管理 Tab -->
      <el-tab-pane label="库位管理" name="bin">
        <el-card>
          <template #header>
            <div class="card-header">
              <div>
                <el-select v-model="binWarehouseId" placeholder="选择仓库" @change="fetchBins" style="width: 200px">
                  <el-option v-for="w in warehouseOptions" :key="w.id" :label="w.warehouseName" :value="w.id" />
                </el-select>
              </div>
              <el-button type="primary" @click="showBinAddDialog" :disabled="!binWarehouseId">新增库位</el-button>
            </div>
          </template>
          <el-table :data="binTableData" v-loading="binLoading" stripe>
            <el-table-column prop="id" label="ID" width="80" />
            <el-table-column prop="binName" label="库位名称" width="150" />
            <el-table-column prop="binCode" label="库位编码" width="120" />
            <el-table-column prop="zoneName" label="所属库区" width="120" />
            <el-table-column prop="capacity" label="容量" width="100" />
            <el-table-column label="操作" width="180">
              <template #default="{ row }">
                <el-button size="small" @click="showBinEditDialog(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="deleteBin(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>

        <!-- 库位新增/编辑弹窗 -->
        <el-dialog v-model="binDialogVisible" :title="binIsEdit ? '编辑库位' : '新增库位'" width="500px">
          <el-form :model="binForm" label-width="100px">
            <el-form-item label="库位名称" required>
              <el-input v-model="binForm.binName" placeholder="如 A-01-01" />
            </el-form-item>
            <el-form-item label="库位编码" required>
              <el-input v-model="binForm.binCode" :disabled="binIsEdit" placeholder="如 BIN-A-0101" />
            </el-form-item>
            <el-form-item label="所属库区" required>
              <el-select v-model="binForm.zoneId" placeholder="请选择库区">
                <el-option v-for="z in zoneOptions" :key="z.id" :label="z.zoneName" :value="z.id" />
              </el-select>
            </el-form-item>
            <el-form-item label="容量">
              <el-input-number v-model="binForm.capacity" :min="0" :max="999999" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="binDialogVisible = false">取消</el-button>
            <el-button type="primary" @click="submitBinForm">确认</el-button>
          </template>
        </el-dialog>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

// ========== 仓库管理 ==========
const activeTab = ref('warehouse')
const loading = ref(false)
const tableData = ref([])
const currentPage = ref(1)
const pageSize = ref(20)
const total = ref(0)
const dialogVisible = ref(false)
const isEdit = ref(false)
const warehouseOptions = ref([])
const form = reactive({
  id: null, warehouseName: '', warehouseCode: '', warehouseType: null,
  stockMode: null, merchantId: '', contactName: '', contactPhone: '', address: ''
})

const fetchData = async () => {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/warehouses', {
      params: { page: currentPage.value, size: pageSize.value }
    })
    if (data.code === 200) {
      tableData.value = data.data.records || []
      total.value = data.data.total || 0
    }
  } finally { loading.value = false }
}

const fetchWarehouseOptions = async () => {
  try {
    const { data } = await axios.get('/api/v1/admin/warehouses', { params: { page: 1, size: 999 } })
    if (data.code === 200) {
      warehouseOptions.value = data.data.records || []
    }
  } catch { /* ignore */ }
}

const showAddDialog = () => {
  isEdit.value = false
  Object.assign(form, { id: null, warehouseName: '', warehouseCode: '', warehouseType: null, stockMode: null, merchantId: '', contactName: '', contactPhone: '', address: '' })
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
      await axios.put(`/api/v1/admin/warehouses/${form.id}`, form)
    } else {
      await axios.post('/api/v1/admin/warehouses', form)
    }
    ElMessage.success(isEdit.value ? '编辑成功' : '新增成功')
    dialogVisible.value = false
    fetchData()
    fetchWarehouseOptions()
  } catch (e) { ElMessage.error('操作失败') }
}

const toggleStatus = async (row) => {
  const newStatus = row.status === 1 ? 0 : 1
  await axios.put(`/api/v1/admin/warehouses/${row.id}/status`, null, { params: { status: newStatus } })
  ElMessage.success(newStatus === 1 ? '已启用' : '已停用')
  fetchData()
}

const deleteWarehouse = async (row) => {
  await ElMessageBox.confirm(`确定删除 ${row.warehouseName}？`, '确认删除', { type: 'warning' })
  await axios.delete(`/api/v1/admin/warehouses/${row.id}`)
  ElMessage.success('已删除')
  fetchData()
  fetchWarehouseOptions()
}

// ========== 库区管理 ==========
const zoneWarehouseId = ref(null)
const zoneLoading = ref(false)
const zoneTableData = ref([])
const zoneDialogVisible = ref(false)
const zoneIsEdit = ref(false)
const zoneOptions = ref([])
const zoneForm = reactive({
  id: null, zoneName: '', zoneCode: '', zoneType: 1, capacity: 0
})

const fetchZones = async () => {
  if (!zoneWarehouseId.value) return
  zoneLoading.value = true
  try {
    const { data } = await axios.get(`/api/v1/admin/warehouses/${zoneWarehouseId.value}/zones`)
    if (data.code === 200) {
      zoneTableData.value = data.data || []
    }
  } finally { zoneLoading.value = false }
}

const showZoneAddDialog = () => {
  zoneIsEdit.value = false
  Object.assign(zoneForm, { id: null, zoneName: '', zoneCode: '', zoneType: 1, capacity: 0 })
  zoneDialogVisible.value = true
}

const showZoneEditDialog = (row) => {
  zoneIsEdit.value = true
  Object.assign(zoneForm, { ...row })
  zoneDialogVisible.value = true
}

const submitZoneForm = async () => {
  try {
    if (zoneIsEdit.value) {
      await axios.put(`/api/v1/admin/warehouses/${zoneWarehouseId.value}/zones/${zoneForm.id}`, zoneForm)
    } else {
      await axios.post(`/api/v1/admin/warehouses/${zoneWarehouseId.value}/zones`, zoneForm)
    }
    ElMessage.success(zoneIsEdit.value ? '编辑成功' : '新增成功')
    zoneDialogVisible.value = false
    fetchZones()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteZone = async (row) => {
  await ElMessageBox.confirm(`确定删除 ${row.zoneName}？`, '确认删除', { type: 'warning' })
  await axios.delete(`/api/v1/admin/warehouses/${zoneWarehouseId.value}/zones/${row.id}`)
  ElMessage.success('已删除')
  fetchZones()
}

// ========== 库位管理 ==========
const binWarehouseId = ref(null)
const binLoading = ref(false)
const binTableData = ref([])
const binDialogVisible = ref(false)
const binIsEdit = ref(false)
const binForm = reactive({
  id: null, binName: '', binCode: '', zoneId: null, capacity: 0
})

const fetchZonesForWarehouse = async () => {
  if (!binWarehouseId.value) return
  try {
    const { data } = await axios.get(`/api/v1/admin/warehouses/${binWarehouseId.value}/zones`)
    if (data.code === 200) {
      zoneOptions.value = data.data || []
    }
  } catch { /* ignore */ }
}

const fetchBins = async () => {
  if (!binWarehouseId.value) return
  binLoading.value = true
  fetchZonesForWarehouse()
  try {
    const { data } = await axios.get(`/api/v1/admin/warehouses/${binWarehouseId.value}/bins`)
    if (data.code === 200) {
      binTableData.value = data.data || []
    }
  } finally { binLoading.value = false }
}

const showBinAddDialog = () => {
  binIsEdit.value = false
  Object.assign(binForm, { id: null, binName: '', binCode: '', zoneId: null, capacity: 0 })
  binDialogVisible.value = true
}

const showBinEditDialog = (row) => {
  binIsEdit.value = true
  Object.assign(binForm, { ...row })
  binDialogVisible.value = true
}

const submitBinForm = async () => {
  try {
    if (binIsEdit.value) {
      await axios.put(`/api/v1/admin/warehouses/${binWarehouseId.value}/bins/${binForm.id}`, binForm)
    } else {
      await axios.post(`/api/v1/admin/warehouses/${binWarehouseId.value}/bins`, binForm)
    }
    ElMessage.success(binIsEdit.value ? '编辑成功' : '新增成功')
    binDialogVisible.value = false
    fetchBins()
  } catch (e) { ElMessage.error('操作失败') }
}

const deleteBin = async (row) => {
  await ElMessageBox.confirm(`确定删除 ${row.binName}？`, '确认删除', { type: 'warning' })
  await axios.delete(`/api/v1/admin/warehouses/${binWarehouseId.value}/bins/${row.id}`)
  ElMessage.success('已删除')
  fetchBins()
}

onMounted(() => {
  fetchData()
  fetchWarehouseOptions()
})
</script>

<style scoped>
.card-header { display: flex; justify-content: space-between; align-items: center; }
</style>
