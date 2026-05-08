<template>
  <div class="product-form">
    <div class="form-header">
      <el-button text @click="$router.back()"><el-icon><ArrowLeft /></el-icon> 返回</el-button>
      <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>

    <el-card>
      <el-form label-position="top">
        <!-- 基本信息 -->
        <div class="form-grid">
          <el-form-item label="商品名称">
            <el-input v-model="form.spu.name" placeholder="请输入商品名称" />
          </el-form-item>
          <el-form-item label="分类">
            <el-select v-model="form.spu.categoryId" placeholder="选择分类" style="width:100%">
              <el-option v-for="c in categories" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </div>

        <!-- 主图上传 -->
        <el-form-item label="主图">
          <div class="upload-area">
            <div v-if="form.spu.mainImage" class="upload-preview">
              <img :src="imageUrl(form.spu.mainImage)" />
              <el-button type="danger" size="small" class="remove-btn" @click="form.spu.mainImage = ''">×</el-button>
            </div>
            <el-upload
              v-else
              class="upload-trigger"
              :http-request="customUpload"
              :show-file-list="false"
              :on-success="onMainImageUploaded"
              :before-upload="beforeUpload"
              accept="image/*"
            >
              <el-icon :size="28"><Plus /></el-icon>
              <span>上传图片</span>
            </el-upload>
          </div>
        </el-form-item>

        <el-form-item label="简介">
          <el-input v-model="form.spu.description" type="textarea" :rows="2" placeholder="商品简介" />
        </el-form-item>
        <el-form-item label="详情">
          <el-input v-model="form.spu.detail" type="textarea" :rows="4" placeholder="商品详情描述" />
        </el-form-item>

        <!-- SKU 规格 -->
        <div class="section-title">SKU 规格</div>
        <div class="sku-list">
          <div v-for="(sku, idx) in form.skus" :key="idx" class="sku-card">
            <div class="sku-header">
              <span class="sku-index">SKU #{{ idx + 1 }}</span>
              <el-button v-if="form.skus.length > 1" text type="danger" size="small" @click="form.skus.splice(idx,1)">删除</el-button>
            </div>

            <!-- SKU 图片 -->
            <div class="sku-image-row">
              <span class="sku-label">图片</span>
              <div class="upload-area small">
                <div v-if="sku.image" class="upload-preview small">
                  <img :src="imageUrl(sku.image)" />
                  <el-button type="danger" size="small" class="remove-btn" @click="sku.image = ''">×</el-button>
                </div>
                <el-upload
                  v-else
                  class="upload-trigger small"
                  :http-request="customUpload"
                  :show-file-list="false"
                  :on-success="(res) => { if (res.code === 200) sku.image = res.data }"
                  :before-upload="beforeUpload"
                  accept="image/*"
                >
                  <el-icon :size="20"><Plus /></el-icon>
                </el-upload>
              </div>
            </div>

            <!-- 规格编辑器: 键值对 -->
            <div class="spec-editor">
              <span class="sku-label">规格</span>
              <div class="spec-pairs">
                <div v-for="(pair, pi) in sku.specPairs" :key="pi" class="spec-pair">
                  <el-input v-model="pair.key" placeholder="属性名" class="spec-key" @change="syncSpec(idx)" />
                  <span class="spec-sep">:</span>
                  <el-input v-model="pair.value" placeholder="属性值" class="spec-val" @change="syncSpec(idx)" />
                  <el-button text type="danger" size="small" @click="removeSpecPair(idx, pi)" :disabled="sku.specPairs.length <= 1">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
                <el-button size="small" class="add-spec-btn" @click="addSpecPair(idx)">+ 添加属性</el-button>
              </div>
            </div>

            <!-- SKU 名称 (自动从规格生成，也可手动改) -->
            <el-input v-model="sku.name" placeholder="SKU 名称（如：128GB 黑色）" class="sku-name-input" />

            <!-- 价格 -->
            <div class="price-row">
              <el-input v-model="sku.price" placeholder="售价" type="number" class="price-input">
                <template #prepend>售价</template>
              </el-input>
              <el-input v-model="sku.originalPrice" placeholder="原价（选填）" type="number" class="price-input">
                <template #prepend>原价</template>
              </el-input>
            </div>
          </div>
        </div>

        <el-button size="small" class="add-sku-btn" @click="addSku" style="margin-top:12px">
          <el-icon><Plus /></el-icon> 添加 SKU
        </el-button>

        <div class="form-actions">
          <el-button type="primary" @click="submit" :loading="saving" size="large">
            {{ isEdit ? '保存修改' : '创建商品' }}
          </el-button>
          <el-button @click="$router.back()" size="large">取消</el-button>
        </div>
      </el-form>
    </el-card>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'

const route = useRoute()
const router = useRouter()
const isEdit = ref(!!route.params.id)
const saving = ref(false)
const categories = ref([])

const form = reactive({
  spu: { name: '', categoryId: null, mainImage: '', description: '', detail: '', images: '' },
  skus: []
})

function emptySku() {
  return { name: '', spec: '{}', specPairs: [{ key: '', value: '' }], price: '', originalPrice: '', image: '' }
}

function addSku() {
  form.skus.push(emptySku())
}

// specPairs ↔ spec JSON 同步
function syncSpec(idx) {
  const pairs = form.skus[idx].specPairs
  const obj = {}
  for (const p of pairs) {
    if (p.key.trim()) {
      obj[p.key.trim()] = p.value.trim()
    }
  }
  form.skus[idx].spec = JSON.stringify(obj)
}

function addSpecPair(idx) {
  form.skus[idx].specPairs.push({ key: '', value: '' })
}

function removeSpecPair(idx, pi) {
  form.skus[idx].specPairs.splice(pi, 1)
  syncSpec(idx)
}

// 文件上传
async function customUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const { data: uploadRes } = await axios.post('/api/v1/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (uploadRes.code === 200) {
      const objectName = uploadRes.data
      // 立刻获取真实URL
      const { data: urlRes } = await axios.get(`/api/v1/files/${objectName}/url`)
      if (urlRes.code === 200) {
        options.onSuccess({ code: 200, data: urlRes.data })
      } else {
        options.onSuccess({ code: 200, data: objectName })
      }
    } else {
      options.onSuccess(uploadRes)
    }
  } catch (e) {
    options.onError(e)
  }
}

const ALLOWED_IMG_TYPES = ['image/jpeg', 'image/png', 'image/gif', 'image/webp', 'image/bmp', 'image/avif']

function beforeUpload(file) {
  const isValid = ALLOWED_IMG_TYPES.includes(file.type)
  const isLt10M = file.size / 1024 / 1024 < 10
  if (!isValid) { ElMessage.error('仅支持 JPG / PNG / GIF / WebP / BMP 格式'); return false }
  if (!isLt10M) { ElMessage.error('图片大小不能超过 10MB'); return false }
  return true
}

function onMainImageUploaded(res) {
  if (res.code === 200) {
    form.spu.mainImage = res.data // 存 objectName，显示时拼 URL
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function imageUrl(objectName) {
  if (!objectName) return ''
  if (objectName.startsWith('http')) return objectName
  return `/api/v1/files/${objectName}/url`
}

onMounted(async () => {
  if (form.skus.length === 0) form.skus.push(emptySku())

  const { data } = await axios.get('/api/v1/categories')
  if (data.code === 200) categories.value = data.data

  if (isEdit.value) {
    const { data: detail } = await axios.get(`/api/v1/products/${route.params.id}`)
    if (detail.code === 200) {
      const d = detail.data
      form.spu = {
        name: d.spu.name || '',
        categoryId: d.spu.categoryId || null,
        mainImage: d.spu.mainImage || '',
        description: d.spu.description || '',
        detail: d.spu.detail || '',
        images: d.spu.images || ''
      }
      form.skus = (d.skus || []).map(s => {
        let pairs = []
        try {
          const obj = typeof s.spec === 'string' ? JSON.parse(s.spec) : (s.spec || {})
          pairs = Object.entries(obj).map(([k, v]) => ({ key: k, value: v }))
        } catch (e) { /* ignore */ }
        if (pairs.length === 0) pairs = [{ key: '', value: '' }]
        return {
          name: s.name || '',
          spec: typeof s.spec === 'string' ? s.spec : JSON.stringify(s.spec || {}),
          specPairs: pairs,
          price: s.price || '',
          originalPrice: s.originalPrice || '',
          image: s.image || ''
        }
      })
    }
  }
})

async function submit() {
  saving.value = true
  try {
    const payload = {
      spu: {
        name: form.spu.name,
        categoryId: form.spu.categoryId,
        brandId: form.spu.brandId,
        description: form.spu.description,
        mainImage: form.spu.mainImage,
        images: form.spu.images,
        detail: form.spu.detail
      },
      skus: form.skus.map(s => ({
        name: s.name,
        spec: s.spec,
        price: s.price,
        originalPrice: s.originalPrice || null,
        image: s.image
      }))
    }

    if (isEdit.value) {
      await axios.put(`/api/v1/admin/products/${route.params.id}`, payload.spu)
    } else {
      await axios.post('/api/v1/admin/products', payload)
    }
    ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
    router.push('/products')
  } catch (e) {
    ElMessage.error('操作失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.product-form { max-width: 860px; }
.form-header { display: flex; align-items: center; gap: 16px; margin-bottom: 20px; }
.form-header h2 { font-size: 20px; font-weight: 600; color: var(--text-primary); }
.form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 0 20px; }

/* Upload */
.upload-area { position: relative; }
.upload-area.small { display: inline-block; }
.upload-preview { position: relative; display: inline-block; }
.upload-preview img { max-width: 200px; max-height: 200px; border: 2px solid var(--border-default); object-fit: cover; }
.upload-preview.small img { max-width: 80px; max-height: 80px; }
.remove-btn { position: absolute; top: -8px; right: -8px; padding: 2px 6px !important; min-height: unset !important; }
.upload-trigger {
  border: 2px dashed var(--border-default) !important;
  width: 200px; height: 200px;
  display: flex !important; flex-direction: column; align-items: center; justify-content: center;
  cursor: pointer; color: var(--text-muted); gap: 8px; font-size: 13px;
  transition: border-color var(--transition);
}
.upload-trigger:hover { border-color: var(--text-primary) !important; color: var(--text-primary); }
.upload-trigger.small { width: 80px; height: 80px; gap: 2px; font-size: 11px; }

/* SKU */
.section-title {
  font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.08em;
  color: var(--text-muted); margin: 28px 0 16px; padding-top: 16px;
  border-top: 1px solid var(--border-subtle);
}
.sku-list { display: flex; flex-direction: column; gap: 16px; }
.sku-card {
  border: 2px solid var(--border-subtle); padding: 20px;
  transition: border-color var(--transition);
}
.sku-card:hover { border-color: var(--border-default); }
.sku-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 14px; }
.sku-index { font-weight: 700; font-size: 13px; color: var(--text-primary); }

.sku-label { font-size: 11px; font-weight: 600; text-transform: uppercase; letter-spacing: 0.06em; color: var(--text-muted); margin-bottom: 6px; display: block; }

.sku-image-row { margin-bottom: 12px; }

/* Spec editor */
.spec-editor { margin-bottom: 12px; }
.spec-pairs { display: flex; flex-direction: column; gap: 6px; }
.spec-pair { display: flex; align-items: center; gap: 8px; }
.spec-key { width: 120px; }
.spec-sep { color: var(--text-muted); font-weight: 600; }
.spec-val { width: 160px; }
.add-spec-btn { margin-top: 4px; }

.sku-name-input { margin-bottom: 10px; }
.price-row { display: flex; gap: 10px; }
.price-input { flex: 1; }

.add-sku-btn { margin-top: 8px; }
.form-actions { margin-top: 28px; display: flex; gap: 10px; }
</style>
