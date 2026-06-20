<template>
  <div class="batch-ship-page">
    <el-card>
      <template #header><span>批量发货</span></template>

      <el-tabs v-model="inputMode">
        <el-tab-pane label="手动输入" name="manual">
          <el-input v-model="rawText" type="textarea" :rows="8"
            placeholder="每行一条：订单ID,物流公司ID,运单号,重量(克)&#10;例如：&#10;1001,1,SF1234567890,1500&#10;1002,2,YT9876543210,2000" />
        </el-tab-pane>
        <el-tab-pane label="上传文件" name="file">
          <el-upload :auto-upload="false" :on-change="handleFileChange" :limit="1" accept=".csv,.txt"
            drag>
            <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
            <div class="el-upload__text">拖拽文件到此处或点击上传</div>
            <template #tip>
              <div class="el-upload__tip">支持 CSV/TXT 格式，每行：订单ID,物流公司ID,运单号,重量(克)</div>
            </template>
          </el-upload>
        </el-tab-pane>
      </el-tabs>

      <div style="margin-top: 12px; display: flex; gap: 16px; align-items: center;">
        <span>默认物流公司：</span>
        <el-select v-model="defaultProviderId" placeholder="选择物流公司" clearable style="width: 220px;" @focus="loadProviders">
          <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
        </el-select>
        <el-button type="primary" @click="parseInput">解析数据</el-button>
      </div>
    </el-card>

    <el-card v-if="parsedItems.length > 0" style="margin-top: 16px;">
      <template #header>
        <span>预览数据</span>
        <span style="margin-left: 8px; color: #909399; font-size: 13px;">共 {{ parsedItems.length }} 条</span>
      </template>

      <el-table :data="parsedItems" stripe max-height="400">
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="orderId" label="订单ID" width="120" />
        <el-table-column label="物流公司" width="180">
          <template #default="{ row }">
            <el-select v-model="row.providerId" placeholder="选择" style="width: 150px;" @focus="loadProviders">
              <el-option v-for="p in providers" :key="p.id" :label="p.providerName" :value="p.id" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column prop="trackingNo" label="运单号" width="180">
          <template #default="{ row }">
            <el-input v-model="row.trackingNo" placeholder="运单号" size="small" />
          </template>
        </el-table-column>
        <el-table-column prop="packageWeight" label="重量(克)" width="120">
          <template #default="{ row }">
            <el-input-number v-model="row.packageWeight" :min="0" :max="50000" size="small" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80">
          <template #default="{ $index }">
            <el-button type="danger" size="small" text @click="parsedItems.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <div style="margin-top: 16px; display: flex; gap: 12px;">
        <el-button type="success" :loading="submitting" @click="submitBatch">提交批量发货</el-button>
        <el-button @click="parsedItems = []; results = null">清空</el-button>
      </div>
    </el-card>

    <el-card v-if="results" style="margin-top: 16px;">
      <template #header><span>提交结果</span></template>
      <el-alert :type="results.errorCount > 0 ? 'warning' : 'success'" :closable="false" show-icon>
        <template #title>
          成功 {{ results.successCount }} 条{{ results.errorCount > 0 ? `，失败 ${results.errorCount} 条` : '' }}
        </template>
      </el-alert>
      <el-table v-if="results.errors.length > 0" :data="results.errors" style="margin-top: 12px;" stripe>
        <el-table-column type="index" label="#" width="50" />
        <el-table-column prop="orderId" label="订单ID" width="120" />
        <el-table-column prop="reason" label="失败原因" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { UploadFilled } from '@element-plus/icons-vue'
import axios from 'axios'

const inputMode = ref('manual')
const rawText = ref('')
const defaultProviderId = ref(null)
const parsedItems = ref([])
const providers = ref([])
const results = ref(null)
const submitting = ref(false)

const loadProviders = async () => {
  if (providers.value.length > 0) return
  try {
    const { data } = await axios.get('/api/v1/admin/logistics/providers/all')
    if (data.code === 200) providers.value = data.data || []
  } catch { /* ignore */ }
}

const handleFileChange = (file) => {
  const reader = new FileReader()
  reader.onload = (e) => {
    rawText.value = e.target.result
    inputMode.value = 'manual'
  }
  reader.readAsText(file.raw)
}

const parseInput = async () => {
  await loadProviders()
  const lines = rawText.value.split('\n').filter(l => l.trim())
  const items = []
  for (const line of lines) {
    const parts = line.split(/[,\t]/).map(s => s.trim())
    if (parts.length < 3) continue
    const orderId = parseInt(parts[0])
    const providerId = parseInt(parts[1])
    const trackingNo = parts[2]
    const packageWeight = parts.length >= 4 ? parseInt(parts[3]) : 0
    if (isNaN(orderId) || isNaN(providerId) || !trackingNo) continue
    items.push({
      orderId,
      providerId: isNaN(providerId) ? defaultProviderId.value : providerId,
      trackingNo,
      packageWeight: isNaN(packageWeight) ? 0 : packageWeight
    })
  }

  // apply default provider for rows without one
  if (defaultProviderId.value) {
    items.forEach(item => { if (!item.providerId) item.providerId = defaultProviderId.value })
  }

  parsedItems.value = items
  results.value = null
  ElMessage.success(`解析完成，共 ${items.length} 条`)
}

const submitBatch = async () => {
  if (parsedItems.value.length === 0) {
    ElMessage.warning('没有可提交的数据')
    return
  }

  // validate
  const invalid = parsedItems.value.filter(i => !i.orderId || !i.providerId || !i.trackingNo)
  if (invalid.length > 0) {
    ElMessage.error(`有 ${invalid.length} 条数据不完整，请检查`)
    return
  }

  submitting.value = true
  try {
    const { data } = await axios.post('/api/v1/admin/logistics/shipping/batch', {
      items: parsedItems.value
    })
    if (data.code === 200) {
      const list = data.data || []
      const successList = list.filter(v => v.id)
      const errorList = parsedItems.value.filter((_, i) => !list[i] || !list[i].id)
      results.value = {
        successCount: successList.length,
        errorCount: errorList.length,
        errors: errorList.map(e => ({ orderId: e.orderId, reason: '发货失败，请检查参数' }))
      }
      if (errorList.length === 0) {
        ElMessage.success(`全部成功，共 ${successList.length} 条`)
        parsedItems.value = []
      } else {
        ElMessage.warning(`成功 ${successList.length} 条，失败 ${errorList.length} 条`)
      }
    } else {
      ElMessage.error(data.message || '提交失败')
    }
  } catch (e) {
    ElMessage.error('批量发货请求失败')
  } finally {
    submitting.value = false
  }
}
</script>
