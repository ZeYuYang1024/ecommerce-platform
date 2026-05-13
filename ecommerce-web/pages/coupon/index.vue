<template>
  <div class="max-w-3xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900 mb-8">优惠券中心</h1>

    <section class="mb-12">
      <h2 class="text-lg font-semibold text-gray-800 mb-4">可领取优惠券</h2>
      <div v-if="loading" class="text-center py-8 text-gray-400">加载中...</div>
      <div v-else-if="available.length===0" class="text-center py-8 text-gray-400">暂无可领取的优惠券</div>
      <div v-else class="grid gap-4">
        <div v-for="t in available" :key="t.id" class="bg-white rounded-2xl border border-gray-100 p-5 flex justify-between items-center">
          <div>
            <div class="font-medium text-gray-900">{{ t.name }}</div>
            <div class="text-sm text-gray-400 mt-1">{{ typeLabel(t.type) }} · {{ discountText(t) }}</div>
            <div class="text-xs text-gray-300 mt-1">有效期至 {{ t.endTime?.substring(0,10) }}</div>
          </div>
          <button @click="claim(t.id)" class="px-5 py-2 bg-amber-500 hover:bg-amber-600 text-white text-sm font-medium rounded-lg transition-colors">立即领取</button>
        </div>
      </div>
      <Pagination v-model:page="availablePage" v-model:size="size" :total="availableTotal" @change="fetchAvailable" />
    </section>

    <section>
      <h2 class="text-lg font-semibold text-gray-800 mb-4">我的优惠券</h2>
      <div v-if="!auth.isLogin" class="text-center py-8 text-gray-400">请先登录</div>
      <div v-else-if="myCoupons.length===0" class="text-center py-8 text-gray-400">暂无优惠券</div>
      <div v-else class="grid gap-4">
        <div v-for="c in myCoupons" :key="c.userCouponId" class="bg-white rounded-2xl border border-gray-100 p-5 flex justify-between items-center" :class="{'opacity-50': c.status !== 0}">
          <div>
            <div class="font-medium text-gray-900">{{ c.name }}</div>
            <div class="text-sm text-gray-400 mt-1">{{ discountText(c) }}</div>
            <div class="text-xs mt-1" :class="c.status===0?'text-green-500':'text-gray-300'">{{ statusLabel(c.status) }}</div>
          </div>
        </div>
      </div>
      <Pagination v-model:page="myPage" v-model:size="size" :total="myTotal" @change="fetchMy" />
    </section>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const available = ref<any[]>([])
const myCoupons = ref<any[]>([])
const loading = ref(true)
const size = ref(10)

const availablePage = ref(1)
const availableTotal = ref(0)

const myPage = ref(1)
const myTotal = ref(0)

function typeLabel(t: string) { return {FULL_REDUCTION:'满减',DISCOUNT:'折扣',FLAT:'立减'}[t]||t }
function discountText(c: any) {
  if (c.type==='FLAT') return `立减 ¥${c.discountAmount}`
  if (c.type==='DISCOUNT') return `${(c.discountRate*100).toFixed(0)}折`
  return `满${c.minAmount}减${c.discountAmount}`
}
function statusLabel(s: number) { return ['可使用','已使用','已过期'][s]||'' }

onMounted(async () => {
  await fetchAvailable()
  if (auth.isLogin) await fetchMy()
})

async function fetchAvailable() {
  loading.value = true
  try {
    const api = useApi()
    const res: any = await api.get('/coupons', { page: availablePage.value, size: size.value })
    if (res.code === 200) {
      available.value = res.data?.records || []
      availableTotal.value = res.data?.total || 0
    }
  } finally { loading.value = false }
}

async function fetchMy() {
  const api = useApi()
  try {
    const res: any = await api.get('/coupons', { page: myPage.value, size: size.value })
    if (res.code === 200) {
      myCoupons.value = res.data?.records || []
      myTotal.value = res.data?.total || 0
    }
  } catch {}
}

async function claim(templateId: number) {
  if (!auth.isLogin) { navigateTo('/login'); return }
  try {
    const api = useApi()
    await api.post(`/coupons/claim?templateId=${templateId}`)
    alert('领取成功')
  } catch { alert('领取失败') }
}
</script>
