<template>
  <div class="register-page">
    <div class="register-card">
      <div class="brand">
        <span class="brand-text">MERCH</span><span class="brand-accent">.</span>
      </div>
      <h1>商家入驻</h1>
      <p class="subtitle">填写信息，提交审核后即可开店</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="register-form">
        <el-form-item label="店铺名称" prop="name">
          <el-input v-model="form.name" placeholder="您的店铺叫什么？" size="large" maxlength="128" show-word-limit />
        </el-form-item>
        <el-form-item label="联系人" prop="contactName">
          <el-input v-model="form.contactName" placeholder="联系人姓名" size="large" />
        </el-form-item>
        <el-form-item label="联系电话" prop="contactPhone">
          <el-input v-model="form.contactPhone" placeholder="手机号码" size="large" />
        </el-form-item>
        <el-form-item label="营业执照 URL" prop="businessLicense">
          <el-input v-model="form.businessLicense" placeholder="营业执照图片链接" size="large" />
        </el-form-item>
        <el-form-item label="店铺 Logo URL（选填）">
          <el-input v-model="form.logo" placeholder="店铺 Logo 图片链接" size="large" />
        </el-form-item>

        <el-button type="primary" size="large" class="submit-btn" @click="submit" :loading="submitting">
          {{ submitting ? '提交中...' : '提交入驻申请' }}
        </el-button>
      </el-form>

      <div v-if="submitted" class="success-box">
        <el-result icon="success" title="申请已提交" sub-title="请耐心等待平台审核，审核通过后即可登录管理店铺">
          <template #extra>
            <el-button type="primary" @click="$router.push('/login')">去登录</el-button>
          </template>
        </el-result>
      </div>

      <div class="footer-link">
        已有账号？<router-link to="/login">去登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const formRef = ref(null)
const submitting = ref(false)
const submitted = ref(false)
const form = reactive({
  name: '', contactName: '', contactPhone: '', businessLicense: '', logo: ''
})
const rules = {
  name: [{ required: true, message: '请输入店铺名称', trigger: 'blur' }],
  contactName: [{ required: true, message: '请输入联系人', trigger: 'blur' }],
  contactPhone: [{ required: true, message: '请输入联系电话', trigger: 'blur' }],
  businessLicense: [{ required: true, message: '请输入营业执照URL', trigger: 'blur' }],
}

async function submit() {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  submitting.value = true
  try {
    const { data } = await axios.post('/api/v1/merchants/register', form)
    if (data.code === 200) {
      submitted.value = true
    } else {
      ElMessage.error(data.message || '提交失败')
    }
  } catch {
    ElMessage.error('网络错误，请重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-page {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #f5f0e8 0%, #faf8f4 50%, #f0ebe0 100%);
  padding: 40px 20px;
}
.register-card {
  background: #fff;
  border-radius: 20px;
  padding: 48px 40px;
  width: 100%;
  max-width: 460px;
  box-shadow: 0 4px 40px rgba(0,0,0,0.06);
}
.brand { text-align: center; margin-bottom: 8px; }
.brand-text { font-size: 24px; font-weight: 800; letter-spacing: 0.04em; color: #1a1816; }
.brand-accent { color: #c8963e; }
h1 { text-align: center; font-size: 22px; font-weight: 700; color: #1a1816; margin: 8px 0 4px; }
.subtitle { text-align: center; font-size: 14px; color: #9ca3af; margin-bottom: 32px; }
.register-form { margin-top: 8px; }
.submit-btn { width: 100%; height: 48px; font-size: 16px; font-weight: 600; margin-top: 8px; }
.success-box { margin-top: 24px; }
.footer-link { text-align: center; margin-top: 24px; font-size: 14px; color: #9ca3af; }
.footer-link a { color: #c8963e; font-weight: 500; text-decoration: none; }
.footer-link a:hover { text-decoration: underline; }
</style>
