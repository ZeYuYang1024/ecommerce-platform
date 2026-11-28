<template>
  <section v-if="data?.application || data?.canApply || loadError" class="rounded-2xl border border-gray-100 bg-white p-6">
    <h2 class="mb-4 font-medium text-gray-900">售后</h2>
    <div v-if="data?.application" class="space-y-2 text-sm text-gray-600">
      <p class="font-medium text-gray-900">{{ statuses[data.application.status] }}</p>
      <p>退款金额 ¥{{ amount }}</p>
      <p>申请原因 {{ data.application.reason }}</p>
      <p v-if="data.application.reviewNote">审核说明 {{ data.application.reviewNote }}</p>
      <p v-if="data.application.failureReason">{{ data.application.failureReason }}</p>
      <p v-if="data.application.refundNo">退款单号 {{ data.application.refundNo }}</p>
      <button v-if="['PENDING', 'REFUNDING'].includes(data.application.status)"
        type="button" class="text-amber-700" @click="load">刷新进度</button>
    </div>
    <form v-else-if="data?.canApply && applying" class="space-y-3" @submit.prevent="submit">
      <p class="text-sm text-gray-600">申请全额退款 ¥{{ amount }}</p>
      <label for="refund-reason" class="block text-sm text-gray-600">退款原因</label>
      <textarea id="refund-reason" v-model.trim="reason" maxlength="256" required rows="3"
        class="w-full rounded-xl border border-gray-200 p-3" />
      <div class="flex gap-4">
        <button type="submit" :disabled="submitting" class="rounded-xl bg-amber-500 px-4 py-2 text-white disabled:bg-gray-300">提交退款申请</button>
        <button type="button" :disabled="submitting" class="text-gray-600" @click="applying = false">返回</button>
      </div>
    </form>
    <button v-else-if="data?.canApply" type="button" class="text-amber-700" @click="applying = true">申请全额退款</button>
    <p v-if="actionError" class="mt-3 text-sm text-red-600" role="alert">{{ actionError }}</p>
    <div v-if="loadError" class="text-sm text-gray-600">
      <p>{{ loadError }}</p>
      <button type="button" class="mt-2 text-amber-700" @click="load">重试售后查询</button>
    </div>
  </section>
</template>

<script setup lang="ts">
type Application = {
  status: 'PENDING' | 'REJECTED' | 'REFUNDING' | 'REFUNDED' | 'FAILED'
  reason: string
  reviewNote?: string
  refundNo?: string
  failureReason?: string
}
type BuyerStatus = { canApply: boolean, application?: Application }
type ApiResult<T> = { code: number, message?: string, data: T }

const props = defineProps<{ orderNo: string, amount: number | string }>()
const emit = defineEmits<{ refunded: [] }>()
const api = useApi()
const data = ref<BuyerStatus>()
const applying = ref(false)
const submitting = ref(false)
const reason = ref('')
const actionError = ref('')
const loadError = ref('')
const statuses: Record<Application['status'], string> = {
  PENDING: '待审核', REJECTED: '已拒绝', REFUNDING: '退款处理中', REFUNDED: '已退款', FAILED: '退款失败'
}

const endpoint = computed(() => `/orders/no/${encodeURIComponent(props.orderNo)}/after-sale`)
onMounted(load)

async function load() {
  loadError.value = ''
  try {
    const result = await api.get<ApiResult<BuyerStatus>>(endpoint.value)
    if (result.code !== 200) throw new Error(result.message)
    data.value = result.data
    if (result.data.application?.status === 'REFUNDED') emit('refunded')
  } catch {
    loadError.value = '暂时无法查询售后进度'
  }
}

async function submit() {
  if (submitting.value || !reason.value) return
  submitting.value = true
  actionError.value = ''
  try {
    const result = await api.post<ApiResult<Application>>(endpoint.value, { reason: reason.value })
    if (result.code !== 200) throw new Error(result.message)
    data.value = { canApply: false, application: result.data }
    applying.value = false
    reason.value = ''
  } catch (error) {
    actionError.value = error instanceof Error ? error.message : '提交退款申请失败'
    await load()
  } finally {
    submitting.value = false
  }
}
</script>
