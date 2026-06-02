<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">个人中心</h1>

    <div class="mt-6 bg-gradient-to-r from-amber-50 to-orange-50 rounded-2xl border border-amber-100 p-6">
      <div class="flex items-center justify-between mb-3">
        <div class="flex items-center gap-3">
          <span class="text-2xl">{{ levelIcon }}</span>
          <div>
            <span class="text-sm text-amber-700 font-semibold">{{ member.level?.name || '普通会员' }}</span>
            <div class="text-xs text-amber-500 mt-0.5">积分倍率 {{ member.level?.pointsMultiplier || '1.00' }}x</div>
          </div>
        </div>
        <div class="text-right">
          <div class="text-2xl font-bold text-amber-700">{{ member.availablePoints || 0 }}</div>
          <div class="text-xs text-amber-500">可用积分</div>
        </div>
      </div>

      <div v-if="member.nextLevelGrowth" class="mt-3">
        <div class="flex justify-between text-xs text-amber-600 mb-1">
          <span>成长值 {{ member.growthValue || 0 }}</span>
          <span>下一等级 {{ member.nextLevelGrowth }}</span>
        </div>
        <div class="w-full bg-amber-200 rounded-full h-2">
          <div class="bg-amber-500 h-2 rounded-full transition-all" :style="{ width: progressPercent + '%' }"></div>
        </div>
      </div>
    </div>

    <div class="mt-3 flex justify-end">
      <button
        class="text-sm px-4 py-2 rounded-lg border border-amber-300 text-amber-700 hover:bg-amber-50 transition-colors disabled:opacity-60 disabled:cursor-not-allowed"
        :disabled="checkedIn"
        @click="doCheckIn"
      >
        {{ checkedIn ? '今日已签到' : `每日签到 +${checkInPoints} 积分` }}
      </button>
    </div>

    <div class="mt-6 bg-white rounded-2xl border border-gray-100 p-6 space-y-4">
      <div class="flex items-center justify-between py-2">
        <span class="text-gray-500">用户名</span>
        <span class="font-medium">{{ auth.username }}</span>
      </div>
      <div class="border-t border-gray-50 pt-4 space-y-3">
        <NuxtLink to="/user/orders" class="flex items-center justify-between py-2 hover:text-amber-600 transition-colors">
          <span>我的订单</span>
          <span class="text-gray-300">→</span>
        </NuxtLink>
        <NuxtLink to="/user/points" class="flex items-center justify-between py-2 hover:text-amber-600 transition-colors">
          <span>积分明细</span>
          <span class="text-gray-300">→</span>
        </NuxtLink>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
const auth = useAuthStore()
const api = useApi()

interface MemberProfile {
  level?: { name: string; levelCode: string; sortOrder: number; pointsMultiplier: number | string }
  growthValue: number
  totalGrowthValue: number
  nextLevelGrowth: number | null
  availablePoints: number
}

const member = ref<MemberProfile>({
  growthValue: 0,
  totalGrowthValue: 0,
  nextLevelGrowth: null,
  availablePoints: 0
})
const checkedIn = ref(false)
const checkInPoints = ref(1)

const levelIcon = computed(() => {
  const map: Record<string, string> = {
    REGULAR: '🥉',
    SILVER: '🥈',
    GOLD: '🥇',
    DIAMOND: '💎'
  }
  return map[member.value.level?.levelCode || ''] || '🥉'
})

const progressPercent = computed(() => {
  if (!member.value.nextLevelGrowth) return 100
  return Math.min(100, Math.round(((member.value.growthValue || 0) / member.value.nextLevelGrowth) * 100))
})

async function fetchMember() {
  try {
    const data = await api.get<any>('/member/profile')
    if (data.code === 200) {
      member.value = data.data || member.value
    }
  } catch {
    // ignore if not logged in
  }
}

async function fetchCheckInStatus() {
  try {
    const data = await api.get<any>('/member/check-in/status')
    if (data.code === 200) {
      checkedIn.value = data.data?.checkedToday || false
      if (data.data?.pointsAwardedToday) {
        checkInPoints.value = data.data.pointsAwardedToday
      }
    }
  } catch {
    // ignore
  }
}

async function doCheckIn() {
  try {
    const data = await api.post<any>('/member/check-in')
    if (data.code === 200) {
      checkedIn.value = true
      checkInPoints.value = data.data?.pointsAwardedToday || 1
      await fetchMember()
    }
  } catch {
    // ignore
  }
}

if (auth.isLoggedIn) {
  fetchMember()
  fetchCheckInStatus()
}
</script>
