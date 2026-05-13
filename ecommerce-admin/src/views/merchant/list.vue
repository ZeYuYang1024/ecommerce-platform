<template>
  <div>
    <div class="toolbar">
      <div class="toolbar-left">
        <el-select v-model="statusFilter" placeholder="审核状态" style="width:160px" clearable @change="statusFilterChange">
          <el-option label="待审核" :value="0" />
          <el-option label="已通过" :value="1" />
          <el-option label="已驳回" :value="2" />
          <el-option label="已关停" :value="3" />
        </el-select>
      </div>
    </div>

    <el-card shadow="never" class="table-card">
      <el-table :data="tableData" v-loading="loading">
        <el-table-column prop="id" label="商家 ID" width="180">
          <template #default="{ row }">
            <span class="font-mono id-text">{{ row.id }}</span>
          </template>
        </el-table-column>
        <el-table-column label="店铺信息" min-width="240">
          <template #default="{ row }">
            <div class="merchant-cell">
              <div class="merchant-avatar">
                <img v-if="row.logo" :src="row.logo" class="logo-img" />
                <span v-else class="logo-placeholder">{{ row.name?.charAt(0) }}</span>
              </div>
              <div>
                <div class="merchant-name">{{ row.name }}</div>
                <div class="merchant-contact">{{ row.contactName }} · {{ row.contactPhone }}</div>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)" size="small">{{ row.statusText }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="申请时间" width="180">
          <template #default="{ row }">
            <span class="font-mono" style="font-size:12px;color:var(--text-secondary)">{{ row.createdAt }}</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <div class="actions">
              <el-button size="small" @click="$router.push(`/merchants/${row.id}`)">查看</el-button>
              <el-button v-if="row.status === 0" size="small" type="primary" @click="openAudit(row)">审核</el-button>
              <el-button v-if="row.status === 1" size="small" type="danger" @click="banMerchant(row)">关停</el-button>
            </div>
          </template>
        </el-table-column>

      </el-table>

      <div class="pagination-row">
          <el-pagination
            v-model:current-page="page"
            v-model:page-size="size"
            :page-sizes="[10, 20, 50, 100]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="fetchData"
          />
        </div>
    </el-card>

    <!-- 审核弹窗 -->
    <el-dialog v-model="auditVisible" title="商家审核" width="520px">
      <div class="audit-info">
        <div class="audit-row"><span class="audit-label">店铺名称</span><span>{{ auditForm.name }}</span></div>
        <div class="audit-row"><span class="audit-label">联系人</span><span>{{ auditForm.contactName }}</span></div>
        <div class="audit-row"><span class="audit-label">联系电话</span><span>{{ auditForm.contactPhone }}</span></div>
        <div class="audit-row" v-if="auditForm.businessLicense">
          <span class="audit-label">营业执照</span>
          <a :href="auditForm.businessLicense" target="_blank" class="license-link">查看执照</a>
        </div>
      </div>
      <div style="margin-top:20px">
        <label class="audit-label" style="margin-bottom:8px;display:block">审核意见</label>
        <el-input v-model="auditComment" type="textarea" :rows="3" placeholder="驳回时请填写原因" />
      </div>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button type="danger" @click="doAudit(2)">驳回</el-button>
        <el-button type="primary" @click="doAudit(1)">通过</el-button>
      </template>
    </el-dialog>

    <!-- 关停确认 -->
    <el-dialog v-model="banVisible" title="关停商家" width="440px">
      <p style="color:var(--text-secondary);margin-bottom:16px">确认关停「{{ banTarget?.name }}」？关停后该商家所有商品将下架。</p>
      <el-input v-model="banReason" type="textarea" :rows="2" placeholder="关停原因（选填）" />
      <template #footer>
        <el-button @click="banVisible = false">取消</el-button>
        <el-button type="danger" @click="doBan">确认关停</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const loading = ref(false)
const tableData = ref([])
const page = ref(1)
const size = ref(10)
const total = ref(0)
const statusFilter = ref(null)

const auditVisible = ref(false)
const auditForm = ref({})
const auditComment = ref('')

const banVisible = ref(false)
const banTarget = ref(null)
const banReason = ref('')

function handleSizeChange() {
  page.value = 1
  fetchData()
}

function statusType(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

async function fetchData() {
  loading.value = true
  try {
    const { data } = await axios.get('/api/v1/admin/merchants', {
      params: { status: statusFilter.value ?? undefined, page: page.value, size: size.value }
    })
    if (data.code === 200) {
      tableData.value = data.data?.records || []
      total.value = Number(data.data?.total) || tableData.value.length
    }
  } finally { loading.value = false }
}

function openAudit(row) {
  auditForm.value = { ...row }
  auditComment.value = ''
  auditVisible.value = true
}

async function doAudit(action) {
  await axios.put(`/api/v1/admin/merchants/${auditForm.value.id}/audit`, {
    action,
    comment: auditComment.value
  })
  auditVisible.value = false
  fetchData()
}

function banMerchant(row) {
  banTarget.value = row
  banReason.value = ''
  banVisible.value = true
}

async function doBan() {
  await axios.put(`/api/v1/admin/merchants/${banTarget.value.id}/audit`, {
    action: 3,
    comment: banReason.value
  })
  banVisible.value = false
  fetchData()
}

function statusFilterChange() {
  page.value = 1
  fetchData()
}

onMounted(fetchData)
</script>

<style scoped>
.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}
.toolbar-left { display: flex; gap: 12px; }

.table-card { border-radius: var(--radius-lg); }

.merchant-cell {
  display: flex;
  align-items: center;
  gap: 14px;
}
.merchant-avatar {
  width: 44px; height: 44px;
  border-radius: var(--radius);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--bg-surface);
  display: flex;
  align-items: center;
  justify-content: center;
}
.logo-img {
  width: 100%; height: 100%;
  object-fit: cover;
}
.logo-placeholder {
  font-size: 18px;
  font-weight: 700;
  color: var(--text-muted);
}
.merchant-name {
  font-weight: 600;
  color: var(--text-primary);
  margin-bottom: 2px;
}
.merchant-contact {
  font-size: 12px;
  color: var(--text-muted);
}

.id-text {
  font-size: 12px;
  color: var(--text-muted);
}
.actions {
  display: flex;
  gap: 4px;
}

.audit-info {
  background: var(--bg-surface);
  border-radius: var(--radius);
  padding: 16px 20px;
}
.audit-row {
  display: flex;
  padding: 6px 0;
}
.audit-label {
  font-size: 13px;
  font-weight: 600;
  color: var(--text-secondary);
  width: 80px;
  flex-shrink: 0;
}
.license-link {
  color: var(--accent);
  font-weight: 500;
  text-decoration: none;
}
.license-link:hover { text-decoration: underline; }
.pagination-row {
  padding: 18px 24px;
  display: flex;
  justify-content: flex-end;
  border-top: 1px solid var(--border-subtle);
  background: var(--bg-card);
}
</style>
