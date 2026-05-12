<template>
  <view class="page">
    <view v-if="loading" class="empty">加载中...</view>

    <view v-else-if="addresses.length" class="address-list">
      <view v-for="item in addresses" :key="item.id" class="address-card">
        <view class="address-head">
          <text class="name">{{ item.receiverName }}</text>
          <text class="phone">{{ item.receiverPhone }}</text>
          <text v-if="item.isDefault === 1" class="tag">默认</text>
        </view>
        <text class="detail">{{ fullAddress(item) }}</text>
        <view class="actions">
          <text class="action" @click="setDefault(item)">设为默认</text>
          <text class="action danger" @click="removeAddress(item)">删除</text>
        </view>
      </view>
    </view>

    <view v-else class="empty">
      <text>暂无收货地址</text>
      <text class="empty-tip">添加地址后，下单时可快速选择</text>
    </view>

    <view class="form-card">
      <text class="form-title">新增收货地址</text>
      <input v-model="form.receiverName" class="input" placeholder="收货人姓名" />
      <input v-model="form.receiverPhone" class="input" placeholder="手机号" />
      <view class="grid">
        <input v-model="form.province" class="input" placeholder="省份" />
        <input v-model="form.city" class="input" placeholder="城市" />
        <input v-model="form.district" class="input" placeholder="区县" />
      </view>
      <input v-model="form.detail" class="input" placeholder="详细地址" />
      <label class="default-row">
        <checkbox :checked="form.isDefault === 1" @click="toggleDefault" />
        <text>设为默认地址</text>
      </label>
      <button class="btn-primary" :disabled="submitting" @click="createAddress">{{ submitting ? '保存中...' : '保存地址' }}</button>
    </view>
  </view>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { request } from '@/utils/api'

const loading = ref(false)
const submitting = ref(false)
const addresses = ref([])
const form = ref({
  receiverName: '',
  receiverPhone: '',
  province: '',
  city: '',
  district: '',
  detail: '',
  isDefault: 0
})

onMounted(fetchAddresses)

async function fetchAddresses() {
  loading.value = true
  const res = await request({ url: '/api/v1/users/addresses' })
  if (res.code === 200) {
    addresses.value = res.data || []
  } else {
    uni.showToast({ title: res.message || '获取地址失败', icon: 'none' })
  }
  loading.value = false
}

function fullAddress(item) {
  return [item.province, item.city, item.district, item.detail].filter(Boolean).join('')
}

function toggleDefault() {
  form.value.isDefault = form.value.isDefault === 1 ? 0 : 1
}

function validateForm() {
  if (!form.value.receiverName || !form.value.receiverPhone || !form.value.detail) {
    uni.showToast({ title: '请填写收货人、手机号和详细地址', icon: 'none' })
    return false
  }
  return true
}

async function createAddress() {
  if (!validateForm()) return
  submitting.value = true
  const res = await request({ url: '/api/v1/users/addresses', method: 'POST', data: form.value })
  submitting.value = false
  if (res.code === 200) {
    uni.showToast({ title: '保存成功', icon: 'success' })
    form.value = { receiverName: '', receiverPhone: '', province: '', city: '', district: '', detail: '', isDefault: 0 }
    fetchAddresses()
  } else {
    uni.showToast({ title: res.message || '保存失败', icon: 'none' })
  }
}

async function setDefault(item) {
  if (item.isDefault === 1) return
  const res = await request({ url: `/api/v1/users/addresses/${item.id}/default`, method: 'PUT' })
  if (res.code === 200) {
    uni.showToast({ title: '设置成功', icon: 'success' })
    fetchAddresses()
  } else {
    uni.showToast({ title: res.message || '设置失败', icon: 'none' })
  }
}

async function removeAddress(item) {
  const { confirm } = await uni.showModal({ title: '删除地址', content: '确定删除该收货地址吗？' })
  if (!confirm) return
  const res = await request({ url: `/api/v1/users/addresses/${item.id}`, method: 'DELETE' })
  if (res.code === 200) {
    uni.showToast({ title: '删除成功', icon: 'success' })
    fetchAddresses()
  } else {
    uni.showToast({ title: res.message || '删除失败', icon: 'none' })
  }
}
</script>

<style scoped>
.page { min-height: 100vh; padding: 24rpx; padding-bottom: 48rpx; background: #F8F8F8; box-sizing: border-box; }
.address-list { margin-bottom: 24rpx; }
.address-card { background: #fff; border-radius: 20rpx; padding: 24rpx; margin-bottom: 16rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04); }
.address-head { display: flex; align-items: center; gap: 16rpx; margin-bottom: 12rpx; }
.name { font-size: 30rpx; font-weight: 600; color: #1F2937; }
.phone { font-size: 26rpx; color: #6B7280; }
.tag { padding: 4rpx 12rpx; border-radius: 999rpx; background: #FEF3C7; color: #D97706; font-size: 22rpx; }
.detail { display: block; font-size: 26rpx; line-height: 40rpx; color: #374151; }
.actions { display: flex; justify-content: flex-end; gap: 28rpx; margin-top: 20rpx; padding-top: 18rpx; border-top: 1px solid #F3F4F6; }
.action { font-size: 24rpx; color: #F59E0B; }
.danger { color: #EF4444; }
.empty { display: flex; flex-direction: column; align-items: center; justify-content: center; min-height: 220rpx; color: #9CA3AF; font-size: 28rpx; }
.empty-tip { margin-top: 12rpx; font-size: 24rpx; }
.form-card { background: #fff; border-radius: 24rpx; padding: 24rpx; box-shadow: 0 8rpx 24rpx rgba(15, 23, 42, 0.04); }
.form-title { display: block; margin-bottom: 20rpx; font-size: 30rpx; font-weight: 600; color: #1F2937; }
.input { width: 100%; height: 84rpx; margin-bottom: 16rpx; padding: 0 22rpx; border: 1px solid #E5E7EB; border-radius: 16rpx; background: #fff; box-sizing: border-box; font-size: 26rpx; }
.grid { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12rpx; }
.default-row { display: flex; align-items: center; gap: 12rpx; margin: 8rpx 0 24rpx; color: #6B7280; font-size: 26rpx; }
.btn-primary { width: 100%; height: 84rpx; line-height: 84rpx; border-radius: 42rpx; background: linear-gradient(135deg, #F59E0B, #F97316); color: #fff; font-size: 28rpx; border: none; }
</style>
