<template>
  <el-button @click="show">售后申请</el-button>
  <el-dialog v-model="open" title="售后申请" width="min(920px, 94vw)" destroy-on-close>
    <el-select v-model="filter" aria-label="售后状态" style="width: 165px; margin-bottom: 12px" @change="changeFilter">
      <el-option label="待审核" value="PENDING" />
      <el-option label="全部状态" value="" />
      <el-option label="退款处理中" value="REFUNDING" />
      <el-option label="已退款" value="REFUNDED" />
      <el-option label="已拒绝" value="REJECTED" />
      <el-option label="退款失败" value="FAILED" />
    </el-select>
    <el-table :data="rows" v-loading="loading" empty-text="暂无售后申请">
      <el-table-column prop="orderNo" label="订单号" width="174" />
      <el-table-column prop="amount" label="退款金额" width="100" />
      <el-table-column label="进度" width="118">
        <template #default="{ row }">{{ statuses[row.status] || row.status }}</template>
      </el-table-column>
      <el-table-column prop="reason" label="申请原因" min-width="132" show-overflow-tooltip />
      <el-table-column label="处理结果" min-width="128" show-overflow-tooltip>
        <template #default="{ row }">{{ row.failureReason || row.reviewNote || row.refundNo || '—' }}</template>
      </el-table-column>
      <el-table-column label="操作" width="142">
        <template #default="{ row }">
          <template v-if="row.status === 'PENDING'">
            <el-button link type="success" :disabled="submitting" @click="review(row, true)">同意</el-button>
            <el-button link type="danger" :disabled="submitting" @click="review(row, false)">拒绝</el-button>
          </template>
          <el-button v-else-if="!merchant && row.status === 'FAILED'" link type="warning"
            :disabled="submitting" @click="retry(row)">重试退款</el-button>
        </template>
      </el-table-column>
    </el-table>
    <el-pagination v-model:current-page="page" :page-size="10" :total="total" layout="prev, pager, next"
      @current-change="load" />
  </el-dialog>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'

const props = defineProps({ merchant: Boolean })
const emit = defineEmits(['reviewed'])
const open = ref(false)
const loading = ref(false)
const submitting = ref(false)
const rows = ref([])
const filter = ref('PENDING')
const page = ref(1)
const total = ref(0)
const statuses = {
  PENDING: '待审核', REFUNDING: '退款处理中', REFUNDED: '已退款', REJECTED: '已拒绝', FAILED: '退款失败'
}

function endpoint() {
  return props.merchant ? '/api/v1/admin/merchant/orders/after-sales' : '/api/v1/admin/orders/after-sales'
}

function show() {
  open.value = true
  void load()
}

function changeFilter() {
  page.value = 1
  void load()
}

async function load() {
  loading.value = true
  try {
    const { data } = await axios.get(endpoint(), {
      params: { page: page.value, size: 10, ...(filter.value ? { status: filter.value } : {}) }
    })
    if (data.code !== 200) throw new Error(data.message)
    rows.value = data.data.records
    total.value = Number(data.data.total)
    if (page.value > 1 && !rows.value.length && total.value > 0) {
      page.value = Math.ceil(total.value / 10)
      await load()
    }
  } catch (error) {
    ElMessage.error(error.message || '加载售后申请失败')
  } finally {
    loading.value = false
  }
}

async function review(row, approved) {
  if (submitting.value) return
  let note
  try {
    if (approved) {
      await ElMessageBox.confirm(`同意为订单 ${row.orderNo} 发起 ¥${row.amount} 全额退款？`, '同意退款')
    } else {
      const result = await ElMessageBox.prompt('填写买家可见的拒绝原因', '拒绝申请', {
        inputValidator: value => (value?.trim() && value.trim().length <= 256) || '请输入 1 到 256 个字'
      })
      note = result.value.trim()
    }
  } catch { return }

  submitting.value = true
  try {
    const { data } = await axios.put(`${endpoint()}/${encodeURIComponent(row.id)}/review`, { approved, note })
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success(approved ? '已提交退款处理' : '已拒绝申请')
    emit('reviewed')
  } catch (error) {
    ElMessage.error(error.message || '操作失败，请刷新后重试')
  } finally {
    submitting.value = false
    await load()
  }
}

async function retry(row) {
  if (submitting.value) return
  try {
    await ElMessageBox.confirm(`重新处理订单 ${row.orderNo} 的退款？`, '重试退款')
  } catch { return }

  submitting.value = true
  try {
    const { data } = await axios.post(`${endpoint()}/${encodeURIComponent(row.id)}/retry`)
    if (data.code !== 200) throw new Error(data.message)
    ElMessage.success('已重新提交退款处理')
  } catch (error) {
    ElMessage.error(error.message || '退款重试失败')
  } finally {
    submitting.value = false
    await load()
  }
}
</script>
