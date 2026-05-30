<template>
  <div class="product-form">
    <div class="form-header">
      <el-button text @click="$router.back()" class="back-btn">
        <el-icon><ArrowLeft /></el-icon> 返回
      </el-button>
      <h2>{{ isEdit ? '编辑商品' : '新增商品' }}</h2>
    </div>

    <div class="form-body">
      <el-card shadow="never" class="form-card">
        <template #header><span class="card-title">基本信息</span></template>
        <el-form label-position="top">
          <div class="form-grid-2">
            <el-form-item label="商品名称">
              <el-input v-model="form.spu.name" placeholder="请输入商品名称" size="large" />
            </el-form-item>
            <el-form-item label="商品分类">
              <el-tree-select
                v-model="form.spu.categoryId"
                :data="categories"
                :props="{ children: 'children', label: 'name', value: 'id' }"
                placeholder="请选择分类"
                check-strictly
                style="width: 100%"
              />
            </el-form-item>
          </div>

          <el-form-item label="商品主图">
            <div class="upload-zone">
              <div v-if="form.spu.mainImage" class="upload-preview">
                <img :src="imageUrl(form.spu.mainImage)" />
                <div class="preview-overlay">
                  <el-button circle size="small" type="danger" @click="form.spu.mainImage = ''">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </div>
              </div>
              <el-upload
                v-else
                class="upload-box"
                :http-request="customUpload"
                :show-file-list="false"
                :on-success="onMainImageUploaded"
                :before-upload="beforeUpload"
                accept="image/*"
                drag
              >
                <el-icon :size="32" class="upload-icon"><Plus /></el-icon>
                <span class="upload-text">拖拽图片到这里，或点击上传</span>
                <span class="upload-hint">支持 JPG / PNG / WebP，最大 10MB</span>
              </el-upload>
            </div>
          </el-form-item>

          <el-form-item label="商品简介">
            <el-input v-model="form.spu.description" type="textarea" :rows="2" placeholder="请输入商品简介" />
          </el-form-item>
          <el-form-item label="商品详情">
            <el-input
              v-model="form.spu.detail"
              type="textarea"
              :rows="4"
              placeholder="请输入商品详情描述，支持 HTML"
            />
          </el-form-item>
        </el-form>
      </el-card>

      <el-card shadow="never" class="form-card">
        <template #header>
          <div class="sku-card-header">
            <span class="card-title">SKU 规格</span>
            <el-button size="small" type="primary" plain @click="addSku">
              <el-icon><Plus /></el-icon> 添加 SKU
            </el-button>
          </div>
        </template>

        <div class="sku-list">
          <div v-for="(sku, idx) in form.skus" :key="idx" class="sku-block">
            <div class="sku-block-header">
              <span class="sku-num">SKU #{{ idx + 1 }}</span>
              <el-button
                v-if="form.skus.length > 1"
                text
                type="danger"
                size="small"
                @click="form.skus.splice(idx, 1)"
              >
                移除
              </el-button>
            </div>

            <div class="sku-fields">
              <div class="sku-upload-area">
                <span class="sku-field-label">图片</span>
                <div v-if="sku.image" class="sku-img-preview">
                  <img :src="imageUrl(sku.image)" />
                  <el-button circle size="small" type="danger" class="img-remove" @click="sku.image = ''">
                    <el-icon :size="12"><Close /></el-icon>
                  </el-button>
                </div>
                <el-upload
                  v-else
                  class="sku-upload-box"
                  :http-request="customUpload"
                  :show-file-list="false"
                  :on-success="(res) => onSkuImageUploaded(sku, res)"
                  :before-upload="beforeUpload"
                  accept="image/*"
                >
                  <el-icon :size="20"><Plus /></el-icon>
                </el-upload>
              </div>

              <div class="spec-area">
                <span class="sku-field-label">规格属性</span>
                <div class="spec-pairs">
                  <div v-for="(pair, pi) in sku.specPairs" :key="pi" class="spec-row">
                    <el-input
                      v-model="pair.key"
                      placeholder="属性名"
                      class="spec-key"
                      size="small"
                      @change="syncSpec(idx)"
                    />
                    <span class="spec-sep">:</span>
                    <el-input
                      v-model="pair.value"
                      placeholder="属性值"
                      class="spec-val"
                      size="small"
                      @change="syncSpec(idx)"
                    />
                    <el-button
                      v-if="sku.specPairs.length > 1"
                      text
                      type="danger"
                      size="small"
                      @click="removeSpecPair(idx, pi)"
                    >
                      <el-icon><Delete /></el-icon>
                    </el-button>
                  </div>
                </div>
                <el-button text type="primary" size="small" @click="addSpecPair(idx)">
                  <el-icon><Plus /></el-icon> 添加属性
                </el-button>
              </div>

              <el-input v-model="sku.name" placeholder="SKU 名称，例如 128GB 黑色" class="sku-name" />

              <div class="price-row">
                <el-input v-model="sku.price" placeholder="售价" type="number" class="price-inp">
                  <template #prepend>售价</template>
                </el-input>
                <el-input v-model="sku.originalPrice" placeholder="原价" type="number" class="price-inp">
                  <template #prepend>原价</template>
                </el-input>
              </div>
            </div>
          </div>
        </div>
      </el-card>

      <div class="submit-bar">
        <el-button type="primary" size="large" @click="submit" :loading="saving">
          {{ isEdit ? '保存修改' : '创建商品' }}
        </el-button>
        <el-button size="large" @click="$router.back()">取消</el-button>
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from 'axios'
import { resolveImageUrl, resolveImageUrls } from '@/utils/image'

const route = useRoute()
const router = useRouter()
const isEdit = computed(() => !!route.params.id)
const isMerchantMode = computed(() => route.path.startsWith('/merchant/products'))
const productBaseUrl = computed(() => (isMerchantMode.value ? '/api/v1/admin/merchant/products' : '/api/v1/admin/products'))
const detailUrl = computed(() => (isMerchantMode.value ? `/api/v1/admin/merchant/products/${route.params.id}` : `/api/v1/products/${route.params.id}`))
const listRoute = computed(() => (isMerchantMode.value ? '/merchant/products' : '/products'))
const saving = ref(false)
const categories = ref([])
const imageMap = ref({})

const form = reactive({
  spu: { name: '', categoryId: null, mainImage: '', description: '', detail: '', images: '', brandId: null },
  skus: []
})

function emptySku() {
  return { name: '', spec: '{}', specPairs: [{ key: '', value: '' }], price: '', originalPrice: '', image: '' }
}

function addSku() {
  form.skus.push(emptySku())
}

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

async function customUpload(options) {
  const formData = new FormData()
  formData.append('file', options.file)
  try {
    const { data: uploadRes } = await axios.post('/api/v1/files/upload', formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
    if (uploadRes.code === 200) {
      // 表单里只保存对象名，预览时再通过文件服务解析成可访问地址。
      options.onSuccess({ code: 200, data: uploadRes.data })
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

  if (!isValid) {
    ElMessage.error('仅支持 JPG / PNG / GIF / WebP / BMP / AVIF 图片')
    return false
  }
  if (!isLt10M) {
    ElMessage.error('图片大小不能超过 10MB')
    return false
  }
  return true
}

function syncResolvedImage(objectName, url) {
  imageMap.value = { ...imageMap.value, [objectName]: url }
}

function onMainImageUploaded(res) {
  if (res.code === 200) {
    form.spu.mainImage = res.data
    resolveImageUrl(res.data).then((url) => syncResolvedImage(res.data, url))
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function onSkuImageUploaded(sku, res) {
  if (res.code === 200) {
    sku.image = res.data
    resolveImageUrl(res.data).then((url) => syncResolvedImage(res.data, url))
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

function imageUrl(objectName) {
  if (!objectName) return ''
  return imageMap.value[objectName] || ''
}

onMounted(async () => {
  if (form.skus.length === 0) form.skus.push(emptySku())

  const { data } = await axios.get('/api/v1/categories')
  if (data.code === 200) categories.value = data.data

  if (isEdit.value) {
    const { data: detail } = await axios.get(detailUrl.value)
    if (detail.code === 200) {
      const d = detail.data

      // 编辑态先把已保存的对象名解析成预览地址，避免图片组件直接请求 JSON 接口。
      imageMap.value = await resolveImageUrls([
        d.spu.mainImage,
        ...(d.skus || []).map((s) => s.image)
      ])

      form.spu = {
        name: d.spu.name || '',
        categoryId: d.spu.categoryId || null,
        mainImage: d.spu.mainImage || '',
        description: d.spu.description || '',
        detail: d.spu.detail || '',
        images: d.spu.images || '',
        brandId: d.spu.brandId || null
      }
      form.skus = (d.skus || []).map((s) => {
        let pairs = []
        try {
          const obj = typeof s.spec === 'string' ? JSON.parse(s.spec) : (s.spec || {})
          pairs = Object.entries(obj).map(([k, v]) => ({ key: k, value: v }))
        } catch (e) {
          // ignore malformed spec JSON
        }
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
      skus: form.skus.map((s) => ({
        name: s.name,
        spec: s.spec,
        price: s.price,
        originalPrice: s.originalPrice || null,
        image: s.image
      }))
    }

    if (isEdit.value) {
      await axios.put(`${productBaseUrl.value}/${route.params.id}`, payload.spu)
    } else {
      await axios.post(productBaseUrl.value, payload)
    }
    ElMessage.success(isEdit.value ? '保存成功' : '创建成功')
    router.push(listRoute.value)
  } catch (e) {
    ElMessage.error('保存失败')
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.product-form { max-width: 860px; }
.form-header {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 24px;
}
.form-header h2 {
  font-size: 22px;
  font-weight: 800;
  color: var(--text-primary);
  letter-spacing: -0.02em;
}
.back-btn {
  border: none !important;
  color: var(--text-secondary) !important;
}
.back-btn:hover { color: var(--accent) !important; background: transparent !important; }

.form-body { display: flex; flex-direction: column; gap: 20px; }

.form-card { border-radius: var(--radius-xl) !important; }
.card-title { font-size: 15px; font-weight: 700; color: var(--text-primary); }

.form-grid-2 {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 24px;
}

.upload-zone { position: relative; }
.upload-preview {
  position: relative;
  display: inline-block;
  border-radius: var(--radius-xl);
  overflow: hidden;
}
.upload-preview img {
  max-width: 240px;
  max-height: 240px;
  object-fit: cover;
  display: block;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-xl);
}
.preview-overlay {
  position: absolute;
  top: 8px;
  right: 8px;
  opacity: 0;
  transition: opacity var(--transition-fast);
}
.upload-preview:hover .preview-overlay { opacity: 1; }

.upload-box {
  border: 2px dashed var(--border-default) !important;
  border-radius: var(--radius-xl) !important;
  width: 240px;
  min-height: 160px;
  display: flex !important;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  transition: all var(--transition-fast);
}
.upload-box:hover {
  border-color: var(--accent) !important;
  background: var(--accent-glow);
}
.upload-box :deep(.el-upload-dragger) {
  background: transparent !important;
  border: none !important;
  width: 100%;
  min-height: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
}
.upload-icon { color: var(--text-muted); margin-bottom: 8px; }
.upload-text { font-size: 13px; color: var(--text-secondary); font-weight: 500; }
.upload-hint { font-size: 11px; color: var(--text-muted); margin-top: 4px; }

.sku-card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.sku-list { display: flex; flex-direction: column; gap: 16px; }
.sku-block {
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius-lg);
  padding: 20px;
  transition: border-color var(--transition-fast);
}
.sku-block:hover { border-color: var(--border-default); }
.sku-block-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 16px;
}
.sku-num { font-weight: 700; font-size: 13px; color: var(--text-primary); }

.sku-fields { display: flex; flex-direction: column; gap: 12px; }
.sku-field-label {
  font-size: 11.5px;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 0.05em;
  color: var(--text-muted);
  margin-bottom: 6px;
  display: block;
}

.sku-upload-area { margin-bottom: 4px; }
.sku-img-preview {
  position: relative;
  display: inline-block;
  border-radius: var(--radius);
  overflow: hidden;
}
.sku-img-preview img {
  width: 72px;
  height: 72px;
  object-fit: cover;
  border: 1px solid var(--border-subtle);
  border-radius: var(--radius);
}
.img-remove {
  position: absolute;
  top: -4px;
  right: -4px;
  width: 20px !important;
  height: 20px !important;
  padding: 0 !important;
}
.sku-upload-box {
  width: 72px;
  height: 72px;
  border: 2px dashed var(--border-default) !important;
  border-radius: var(--radius) !important;
  display: flex !important;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  color: var(--text-muted);
  transition: all var(--transition-fast);
}
.sku-upload-box:hover {
  border-color: var(--accent) !important;
  color: var(--accent);
}

.spec-area { margin-bottom: 4px; }
.spec-pairs { display: flex; flex-direction: column; gap: 8px; }
.spec-row { display: flex; align-items: center; gap: 8px; }
.spec-key { width: 120px; }
.spec-sep { color: var(--text-muted); font-weight: 600; font-size: 16px; }
.spec-val { width: 160px; }

.sku-name { margin-bottom: 4px; }

.price-row { display: flex; gap: 10px; }
.price-inp { flex: 1; }

.submit-bar {
  display: flex;
  gap: 10px;
  margin-top: 8px;
}
</style>
