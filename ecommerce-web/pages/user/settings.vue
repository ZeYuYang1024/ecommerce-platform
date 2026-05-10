<template>
  <div class="max-w-2xl mx-auto px-4 py-8">
    <h1 class="text-2xl font-bold text-gray-900">账号设置</h1>

    <div v-if="profile" class="mt-8 bg-white rounded-2xl border border-gray-100 p-8 space-y-6">
      <div class="text-center">
        <div class="w-20 h-20 bg-amber-100 rounded-full flex items-center justify-center mx-auto text-amber-700 text-2xl font-bold">
          {{ profile.username?.charAt(0)?.toUpperCase() }}
        </div>
        <h2 class="text-lg font-bold mt-4">{{ profile.username }}</h2>
        <p class="text-sm text-gray-400">注册时间: {{ profile.createdAt }}</p>
      </div>

      <div class="border-t border-gray-100 pt-6">
        <label class="block text-sm font-medium text-gray-700 mb-2">手机号</label>
        <input v-model="phone" class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-amber-300" placeholder="输入手机号" />
      </div>

      <div>
        <label class="block text-sm font-medium text-gray-700 mb-2">头像 URL</label>
        <input v-model="avatar" class="w-full h-11 px-4 rounded-xl border border-gray-200 text-sm focus:outline-none focus:ring-2 focus:ring-amber-300" placeholder="输入头像链接" />
      </div>

      <button @click="save" :disabled="saving" class="w-full h-11 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-200 text-white font-medium rounded-xl transition-colors">
        {{ saving ? '保存中...' : '保存' }}
      </button>
      <p v-if="saved" class="text-center text-green-600 text-sm">保存成功</p>
    </div>
  </div>
</template>

<script setup lang="ts">
const api = useApi()
const profile = ref<any>(null)
const phone = ref('')
const avatar = ref('')
const saving = ref(false)
const saved = ref(false)

onMounted(async () => {
  try {
    const res: any = await api.get('/auth/me')
    if (res.code === 200) {
      profile.value = res.data
      phone.value = res.data.phone || ''
      avatar.value = res.data.avatar || ''
    }
  } catch {}
})

async function save() {
  saving.value = true
  saved.value = false
  try {
    await api.put('/auth/me', { phone: phone.value, avatar: avatar.value })
    saved.value = true
  } finally { saving.value = false }
}
</script>
