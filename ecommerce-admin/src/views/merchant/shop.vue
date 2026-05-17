<template>
  <div class="shop-page">
    <div class="toolbar">
      <div>
        <span class="section-label">店铺信息</span>
        <p class="page-desc">查看并维护店铺基础资料</p>
      </div>
      <div v-if="shop" class="toolbar-actions">
        <el-button
          v-if="!editing"
          data-testid="shop-edit-button"
          type="primary"
          @click="startEdit"
        >
          编辑信息
        </el-button>
        <template v-else>
          <el-button @click="cancelEdit">取消</el-button>
          <el-button
            data-testid="shop-save-button"
            type="primary"
            :loading="saving"
            @click="save"
          >
            保存修改
          </el-button>
        </template>
      </div>
    </div>

    <el-card shadow="never" class="info-card" v-loading="loading">
      <div v-if="shop" class="shop-info">
        <div class="shop-hero">
          <div class="shop-logo">
            <img v-if="shop.logo" :src="shop.logo" alt="shop logo" />
            <span v-else>{{ shop.name?.charAt(0) }}</span>
          </div>
          <div class="shop-hero-body">
            <h2>{{ shop.name }}</h2>
            <div class="shop-hero-meta">
              <el-tag :type="statusTag(shop.status)" size="small">{{ shop.statusText }}</el-tag>
              <span class="shop-id">商家 ID {{ shop.id }}</span>
            </div>
          </div>
        </div>

        <el-form
          v-if="editing"
          ref="formRef"
          :model="editForm"
          :rules="rules"
          label-position="top"
          class="edit-form"
        >
          <div class="form-grid">
            <el-form-item label="店铺名称" prop="name">
              <el-input
                v-model="editForm.name"
                data-testid="shop-name-input"
                placeholder="请输入店铺名称"
              />
            </el-form-item>
            <el-form-item label="联系人" prop="contactName">
              <el-input
                v-model="editForm.contactName"
                data-testid="shop-contact-name-input"
                placeholder="请输入联系人"
              />
            </el-form-item>
            <el-form-item label="联系电话" prop="contactPhone">
              <el-input
                v-model="editForm.contactPhone"
                data-testid="shop-contact-phone-input"
                placeholder="请输入联系电话"
              />
            </el-form-item>
            <el-form-item label="Logo 地址">
              <el-input
                v-model="editForm.logo"
                data-testid="shop-logo-input"
                placeholder="请输入店铺 Logo 地址"
              />
            </el-form-item>
          </div>
          <el-form-item label="营业执照地址" prop="businessLicense">
            <el-input
              v-model="editForm.businessLicense"
              data-testid="shop-business-license-input"
              placeholder="请输入营业执照地址"
            />
          </el-form-item>
        </el-form>

        <el-descriptions v-else :column="2" border class="info-table">
          <el-descriptions-item label="联系人">{{ shop.contactName }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ shop.contactPhone }}</el-descriptions-item>
          <el-descriptions-item label="入驻时间">{{ formatDateTime(shop.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="店铺 Logo">
            <a
              v-if="shop.logo"
              :href="shop.logo"
              target="_blank"
              rel="noreferrer"
              class="info-link"
            >
              查看 Logo
            </a>
            <span v-else class="muted-text">未设置</span>
          </el-descriptions-item>
          <el-descriptions-item label="营业执照" :span="2">
            <a
              v-if="shop.businessLicense"
              :href="shop.businessLicense"
              target="_blank"
              rel="noreferrer"
              class="info-link"
            >
              查看营业执照
            </a>
            <span v-else class="muted-text">未设置</span>
          </el-descriptions-item>
          <el-descriptions-item v-if="shop.reason" label="备注" :span="2">{{ shop.reason }}</el-descriptions-item>
        </el-descriptions>
      </div>
      <div v-else class="empty">暂无店铺信息</div>
    </el-card>
  </div>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { ensureMerchantContext } from '@/utils/auth'
import { formatDateTime } from '@/utils/dateTime'

const loading = ref(false)
const saving = ref(false)
const editing = ref(false)
const formRef = ref(null)
const shop = ref(null)

const editForm = reactive({
  name: '',
  logo: '',
  contactName: '',
  contactPhone: '',
  businessLicense: ''
})

const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  businessLicense: [{ required: true, message: '请输入营业执照地址', trigger: 'blur' }]
}

function statusTag(status) {
  const map = { 0: 'warning', 1: 'success', 2: 'danger', 3: 'info' }
  return map[status] || 'info'
}

function syncForm(source) {
  editForm.name = source?.name || ''
  editForm.logo = source?.logo || ''
  editForm.contactName = source?.contactName || ''
  editForm.contactPhone = source?.contactPhone || ''
  editForm.businessLicense = source?.businessLicense || ''
}

function resolveMerchantId() {
  return ensureMerchantContext()
}

async function fetchShop() {
  const merchantId = resolveMerchantId()
  if (!merchantId) {
    shop.value = null
    return
  }

  loading.value = true
  try {
    const { data } = await axios.get(`/api/v1/admin/merchants/${merchantId}`)
    if (data.code === 200) {
      shop.value = data.data
      syncForm(shop.value)
    }
  } finally {
    loading.value = false
  }
}

function startEdit() {
  syncForm(shop.value)
  editing.value = true
}

function cancelEdit() {
  syncForm(shop.value)
  editing.value = false
}

async function save() {
  const valid = await formRef.value?.validate().catch(() => false)
  const merchantId = resolveMerchantId()
  if (!valid || !merchantId) return

  saving.value = true
  try {
    const payload = {
      name: editForm.name,
      logo: editForm.logo,
      contactName: editForm.contactName,
      contactPhone: editForm.contactPhone,
      businessLicense: editForm.businessLicense
    }
    const { data } = await axios.put(`/api/v1/admin/merchants/${merchantId}`, payload)
    if (data.code === 200) {
      shop.value = data.data || { ...shop.value, ...payload }
      syncForm(shop.value)
      editing.value = false
      ElMessage.success('店铺信息已更新')
    } else {
      ElMessage.error(data.message || '更新失败')
    }
  } catch {
    ElMessage.error('更新失败')
  } finally {
    saving.value = false
  }
}

onMounted(fetchShop)
</script>

<style scoped>
.shop-page {
  max-width: 920px;
}

.toolbar {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  margin-bottom: 20px;
}

.toolbar-actions {
  display: flex;
  gap: 12px;
  flex-shrink: 0;
}

.page-desc {
  font-size: 13.5px;
  color: var(--text-muted);
  margin-top: 4px;
}

.info-card {
  border-radius: var(--radius-xl);
}

.shop-info {
  display: flex;
  flex-direction: column;
  gap: 28px;
}

.shop-hero {
  display: flex;
  align-items: center;
  gap: 20px;
}

.shop-logo {
  width: 72px;
  height: 72px;
  border-radius: var(--radius-xl);
  overflow: hidden;
  background: linear-gradient(135deg, #059669, #10b981);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 30px;
  font-weight: 700;
  flex-shrink: 0;
}

.shop-logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.shop-hero-body h2 {
  font-size: 24px;
  font-weight: 700;
  color: var(--text-primary);
  margin-bottom: 8px;
}

.shop-hero-meta {
  display: flex;
  align-items: center;
  gap: 12px;
  flex-wrap: wrap;
}

.shop-id {
  font-size: 13px;
  color: var(--text-muted);
}

.form-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 16px;
}

.edit-form :deep(.el-form-item) {
  margin-bottom: 18px;
}

.info-table {
  margin-top: -4px;
}

.info-link {
  color: var(--accent);
  font-weight: 500;
  text-decoration: none;
}

.info-link:hover {
  text-decoration: underline;
}

.muted-text {
  color: var(--text-muted);
}

.empty {
  text-align: center;
  padding: 60px;
  color: var(--text-muted);
}

@media (max-width: 768px) {
  .toolbar {
    flex-direction: column;
  }

  .toolbar-actions {
    width: 100%;
    justify-content: flex-start;
  }

  .form-grid {
    grid-template-columns: 1fr;
  }

  .shop-hero {
    align-items: flex-start;
  }
}
</style>
