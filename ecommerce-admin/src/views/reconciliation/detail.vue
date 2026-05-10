<template>
  <div>
    <div class="back-bar">
      <el-button text @click="$router.push('/reconciliation')">
        <el-icon style="margin-right:4px"><ArrowLeft /></el-icon> 返回对账列表
      </el-button>
    </div>

    <div v-if="rec" class="detail-wrap">
      <div class="summary-card">
        <div class="summary-item">
          <span class="summary-label">批次号</span>
          <span class="font-mono" style="font-size:14px;color:var(--text-primary)">{{ rec.batchNo }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">订单总数</span>
          <span class="summary-val">{{ rec.totalOrderCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">支付单总数</span>
          <span class="summary-val">{{ rec.totalPaymentCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">匹配成功</span>
          <span class="summary-val" style="color:var(--green)">{{ rec.matchedCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">差异</span>
          <span class="summary-val" style="color:var(--red)">{{ rec.unmatchedCount }}</span>
        </div>
        <div class="summary-item">
          <span class="summary-label">状态</span>
          <el-tag :type="rec.status === 1 ? 'success' : 'warning'" size="small">{{ rec.statusText }}</el-tag>
        </div>
      </div>

      <el-card v-if="rec.details && rec.details.length > 0" shadow="never" class="table-card">
        <template #header><span style="font-weight:700">对账明细</span></template>
        <el-table :data="rec.details">
          <el-table-column label="类型" width="100">
            <template #default="{ row }">
              <el-tag :type="row.recordType === 'ORDER' ? '' : 'success'" size="small">{{ row.recordType }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="订单号" width="200">
            <template #default="{ row }">
              <span class="font-mono" style="font-size:12px">{{ row.orderNo || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="支付单号" width="200">
            <template #default="{ row }">
              <span class="font-mono" style="font-size:12px">{{ row.paymentNo || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">
              <span>¥{{ row.amount || '-' }}</span>
            </template>
          </el-table-column>
          <el-table-column label="匹配结果" width="140" align="center">
            <template #default="{ row }">
              <el-tag :type="matchTag(row.matchStatus)" size="small">{{ matchText(row.matchStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="差异原因" min-width="200">
            <template #default="{ row }">
              <span style="font-size:12px;color:var(--text-muted)">{{ row.diffReason || '-' }}</span>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const rec = ref(null)

function matchTag(status) {
  if (status === 'MATCHED') return 'success'
  if (status === 'ORDER_ONLY' || status === 'PAYMENT_ONLY') return 'danger'
  return 'warning'
}

function matchText(status) {
  const map = {
    MATCHED: '匹配',
    ORDER_ONLY: '仅有订单',
    PAYMENT_ONLY: '仅有支付',
    AMOUNT_MISMATCH: '金额不符',
    STATUS_MISMATCH: '状态不符'
  }
  return map[status] || status
}

onMounted(async () => {
  try {
    const { data } = await axios.get(`/api/v1/admin/reconciliation/${route.params.id}`)
    if (data.code === 200) rec.value = data.data
  } catch {}
})
</script>

<style scoped>
.back-bar { margin-bottom: 16px; }
.detail-wrap { max-width: 960px; }
.summary-card {
  background: var(--bg-card);
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
  padding: 24px 32px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 20px;
  margin-bottom: 20px;
}
.summary-item { display: flex; flex-direction: column; gap: 4px; }
.summary-label { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted); }
.summary-val { font-size: 24px; font-weight: 800; font-family: var(--font-mono); color: var(--text-primary); }
.table-card { border-radius: var(--radius-lg); }
</style>
