<template>
  <div class="max-w-4xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">确认支付</h1>

    <div v-if="order" class="mt-8 grid md:grid-cols-3 gap-8">
      <div class="md:col-span-2 space-y-6">
        <div class="bg-white rounded-2xl border border-gray-100 p-6">
          <h3 class="font-medium text-gray-900 mb-4">订单信息</h3>
          <div class="text-sm text-gray-400 font-mono">订单号: {{ order.orderNo }}</div>
          <div v-for="item in order.items" :key="item.id" class="flex items-center gap-4 py-3 border-b border-gray-50 last:border-0">
            <div class="w-12 h-12 bg-gray-50 rounded-lg overflow-hidden flex-shrink-0">
              <img :src="item.resolvedImage || '/placeholder.svg'" class="w-full h-full object-cover" />
            </div>
            <div class="flex-1 min-w-0">
              <div class="text-sm font-medium truncate">{{ item.name }}</div>
              <div class="text-xs text-gray-400">¥{{ item.price }} x {{ item.quantity }}</div>
            </div>
            <div class="text-sm font-medium">¥{{ item.totalPrice }}</div>
          </div>
        </div>
      </div>

      <div class="bg-white rounded-2xl border border-gray-100 p-6 h-fit sticky top-20">
        <div class="text-sm text-gray-500 flex justify-between py-2">
          <span>订单金额</span><span>¥{{ order.totalAmount }}</span>
        </div>
        <div class="border-t border-gray-100 mt-2 pt-4 flex justify-between">
          <span class="font-medium">待支付</span>
          <span class="text-xl font-bold text-amber-600">¥{{ order.totalAmount }}</span>
        </div>
        <button @click="doPay" :disabled="paying" class="mt-6 w-full h-12 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 text-white font-medium rounded-xl transition-colors">
          {{ paying ? '支付中...' : '确认支付' }}
        </button>
        <div v-if="paid" class="mt-4 text-center">
          <div class="text-green-600 text-lg font-bold">支付成功！</div>
          <NuxtLink to="/user/orders" class="text-sm text-amber-600 hover:text-amber-700 mt-2 inline-block">查看订单 →</NuxtLink>
        </div>
        <div v-if="errorMsg" class="mt-4 text-center text-red-500 text-sm">{{ errorMsg }}</div>
      </div>
    </div>

    <div v-else-if="loadError" class="mt-8 text-center py-16">
      <p class="text-gray-400 text-lg">{{ loadError }}</p>
      <NuxtLink to="/user/orders" class="mt-4 inline-block text-amber-600 hover:text-amber-700 font-medium">查看我的订单 →</NuxtLink>
    </div>

    <div v-else class="mt-8 text-center py-16 text-gray-400">
      加载中...
    </div>
  </div>
</template>

<script setup lang="ts">
const route = useRoute()
const api = useApi()
const order = ref<any>(null)
const paying = ref(false)
const paid = ref(false)
const errorMsg = ref('')
const loadError = ref('')
const { primeImageUrls, resolveImageUrl } = useImageUrl()

onMounted(async () => {
  try {
    const res: any = await api.get(`/orders/no/${route.params.orderNo}`)
    if (res.code === 200) {
      order.value = res.data
      const items = order.value?.items || []
      await primeImageUrls(items.map((item: any) => item.image))
      await Promise.all(items.map(async (item: any) => {
        item.resolvedImage = await resolveImageUrl(item.image)
      }))
    } else {
      loadError.value = '订单不存在'
    }
  } catch {
    loadError.value = '加载失败，请稍后重试'
  }
})

async function doPay() {
  paying.value = true
  errorMsg.value = ''
  try {
    const res: any = await api.post('/payment/pay', {
      orderNo: order.value.orderNo,
      orderId: order.value.id,
      amount: order.value.totalAmount,
      payMethod: 'wx_jsapi'
    })
    if (res.code === 200) {
      paid.value = true
    } else {
      errorMsg.value = res.message || '支付失败'
    }
  } catch {
    errorMsg.value = '支付请求失败，请重试'
  } finally {
    paying.value = false
  }
}
</script>
