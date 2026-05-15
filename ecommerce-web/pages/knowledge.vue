<template>
  <div class="min-h-screen bg-gray-50">
    <div class="max-w-3xl mx-auto px-4 py-8">
      <div class="text-center mb-8">
        <div class="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-amber-100 mb-4">
          <svg class="w-8 h-8 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">智能客服助手</h1>
        <p class="mt-2 text-sm text-gray-500">有问题随时问，秒回不等待</p>
      </div>

      <div ref="chatBox" class="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden">
        <div class="h-[500px] overflow-y-auto p-6 space-y-4" ref="msgList">
          <div v-if="messages.length === 0" class="flex flex-col items-center justify-center h-full text-gray-400">
            <svg class="w-12 h-12 mb-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.625 9.75a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375m-13.5 3.01c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.184-4.183a1.14 1.14 0 01.778-.332 48.294 48.294 0 005.83-.498c1.585-.233 2.708-1.626 2.708-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
            </svg>
            <p class="text-sm">输入问题，开始对话</p>
            <div class="mt-4 flex flex-wrap gap-2 justify-center">
              <button v-for="q in quickQuestions" :key="q" @click="send(q)" class="px-3 py-1.5 text-xs bg-gray-100 hover:bg-amber-50 hover:text-amber-700 rounded-full transition-colors">{{ q }}</button>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
            <div :class="['max-w-[80%] px-4 py-3 rounded-2xl text-sm leading-relaxed', msg.role === 'user' ? 'bg-amber-500 text-white rounded-br-md' : 'bg-gray-100 text-gray-800 rounded-bl-md', msg.role === 'assistant' ? 'ai-msg' : '']">
              <div v-if="msg.role === 'assistant' && msg.loading" class="flex items-center gap-1 py-1">
                <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:0s"></span>
                <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:0.15s"></span>
                <span class="w-2 h-2 bg-gray-400 rounded-full animate-bounce" style="animation-delay:0.3s"></span>
              </div>
              <div v-else v-html="msg.role === 'assistant' ? marked.parse(msg.content) : msg.content" />
            </div>
          </div>
        </div>

        <div class="border-t border-gray-100 p-4">
          <div class="flex gap-2">
            <input
              v-model="input"
              @keyup.enter="send()"
              :disabled="sending"
              placeholder="输入您的问题..."
              class="flex-1 h-11 px-4 rounded-xl border border-gray-200 focus:outline-none focus:ring-2 focus:ring-amber-300 focus:border-amber-400 text-sm disabled:bg-gray-50"
            />
            <button
              @click="send()"
              :disabled="sending || !input.trim()"
              class="h-11 px-6 bg-amber-500 hover:bg-amber-600 disabled:bg-gray-300 text-white font-medium rounded-xl transition-colors text-sm"
            >发送</button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { marked } from 'marked'

const messages = ref<{ role: string; content: string; loading?: boolean }[]>([])
const input = ref('')
const sending = ref(false)
const sessionId = ref('')
const msgList = ref<HTMLElement>()
const api = useApi()

const quickQuestions = ['如何退换货？', '优惠券怎么用？', '支付方式有哪些？', '订单多久发货？']

onMounted(() => {
  sessionId.value = localStorage.getItem('kb_session') || ''
})

function scrollBottom() {
  nextTick(() => {
    const el = msgList.value
    if (el) el.scrollTop = el.scrollHeight
  })
}

async function send(quick?: string) {
  const text = quick || input.value.trim()
  if (!text || sending.value) return

  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const aiMsg = { role: 'assistant', content: '', loading: true }
  messages.value.push(aiMsg)
  scrollBottom()

  sending.value = true
  try {
    const res: any = await api.post('/knowledge/chat', {
      question: text,
      sessionId: sessionId.value || undefined
    })
    aiMsg.loading = false
    if (res.code === 0 && res.data) {
      aiMsg.content = res.data.answer || '抱歉，我暂时无法回答这个问题。'
      if (res.data.sessionId) {
        sessionId.value = res.data.sessionId
        localStorage.setItem('kb_session', res.data.sessionId)
      }
    } else {
      aiMsg.content = '抱歉，服务暂时不可用，请稍后再试。'
    }
  } catch (e: any) {
    aiMsg.loading = false
    if (e?.response?.status === 401) {
      aiMsg.content = '请先登录后再使用智能客服。'
    } else {
      aiMsg.content = '网络异常，请稍后再试。'
    }
  } finally {
    sending.value = false
    scrollBottom()
  }
}
</script>

<style scoped>
.ai-msg :deep(h1), .ai-msg :deep(h2), .ai-msg :deep(h3) { font-size: 1em; font-weight: 700; margin: 8px 0 4px; }
.ai-msg :deep(h3) { font-size: 0.95em; }
.ai-msg :deep(p) { margin: 4px 0; }
.ai-msg :deep(ul), .ai-msg :deep(ol) { margin: 4px 0; padding-left: 18px; }
.ai-msg :deep(li) { margin: 2px 0; }
.ai-msg :deep(strong) { font-weight: 600; color: #1f2937; }
.ai-msg :deep(code) { background: #e5e7eb; padding: 1px 4px; border-radius: 4px; font-size: 0.9em; }
</style>
