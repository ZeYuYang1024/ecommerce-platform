<template>
  <div class="h-dvh overflow-hidden bg-gray-50">
    <div class="mx-auto flex h-full max-w-6xl flex-col px-4 py-4 sm:px-6 lg:px-8 lg:py-6">
      <div class="relative mb-4 shrink-0 text-center lg:mb-6">
        <button
          class="absolute left-0 top-0 inline-flex items-center gap-2 rounded-xl px-3 py-2 text-sm font-medium text-gray-600 transition-colors hover:bg-white hover:text-gray-900"
          @click="goBack"
        >
          <svg class="h-4 w-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M15 19l-7-7 7-7" />
          </svg>
          返回
        </button>
        <div class="mb-4 inline-flex h-16 w-16 items-center justify-center rounded-2xl bg-amber-100">
          <svg class="h-8 w-8 text-amber-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
            <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M9.813 15.904L9 18.75l-.813-2.846a4.5 4.5 0 00-3.09-3.09L2.25 12l2.846-.813a4.5 4.5 0 003.09-3.09L9 5.25l.813 2.846a4.5 4.5 0 003.09 3.09L15.75 12l-2.846.813a4.5 4.5 0 00-3.09 3.09zM18.259 8.715L18 9.75l-.259-1.035a3.375 3.375 0 00-2.455-2.456L14.25 6l1.036-.259a3.375 3.375 0 002.455-2.456L18 2.25l.259 1.035a3.375 3.375 0 002.455 2.456L21.75 6l-1.036.259a3.375 3.375 0 00-2.455 2.456z" />
          </svg>
        </div>
        <h1 class="text-2xl font-bold text-gray-900">智能客服助手</h1>
        <p class="mt-2 text-sm text-gray-500">优先基于知识库回答，必要时联动订单、商品和优惠券实时数据。</p>
      </div>

      <div class="flex min-h-0 flex-1 flex-col overflow-hidden rounded-2xl border border-gray-100 bg-white shadow-sm">
        <div ref="msgList" class="min-h-0 flex-1 space-y-4 overflow-y-auto p-6">
          <div v-if="messages.length === 0" class="flex h-full flex-col items-center justify-center text-gray-400">
            <svg class="mb-3 h-12 w-12" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path stroke-linecap="round" stroke-linejoin="round" stroke-width="1.5" d="M8.625 9.75a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H8.25m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0H12m4.125 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm0 0h-.375m-13.5 3.01c0 1.6 1.123 2.994 2.707 3.227 1.087.16 2.185.283 3.293.369V21l4.184-4.183a1.14 1.14 0 01.778-.332 48.294 48.294 0 005.83-.498c1.585-.233 2.708-1.626 2.708-3.228V6.741c0-1.602-1.123-2.995-2.707-3.228A48.394 48.394 0 0012 3c-2.392 0-4.744.175-7.043.513C3.373 3.746 2.25 5.14 2.25 6.741v6.018z" />
            </svg>
            <p class="text-sm">输入问题，开始对话</p>
            <div class="mt-4 flex flex-wrap justify-center gap-2">
              <button
                v-for="question in quickQuestions"
                :key="question"
                class="rounded-full bg-gray-100 px-3 py-1.5 text-xs transition-colors hover:bg-amber-50 hover:text-amber-700"
                @click="send(question)"
              >
                {{ question }}
              </button>
            </div>
          </div>

          <div v-for="(msg, idx) in messages" :key="idx" :class="['flex', msg.role === 'user' ? 'justify-end' : 'justify-start']">
            <div
              :class="[
                'max-w-[88%] rounded-2xl px-4 py-3 text-sm leading-relaxed lg:max-w-[76%]',
                msg.role === 'user' ? 'rounded-br-md bg-amber-500 text-white' : 'rounded-bl-md bg-gray-100 text-gray-800',
                msg.role === 'assistant' ? 'ai-msg' : ''
              ]"
            >
              <div v-if="msg.role === 'assistant' && msg.loading" class="flex items-center gap-1 py-1">
                <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400" style="animation-delay: 0s"></span>
                <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400" style="animation-delay: 0.15s"></span>
                <span class="h-2 w-2 animate-bounce rounded-full bg-gray-400" style="animation-delay: 0.3s"></span>
              </div>
              <template v-else>
                <div v-html="msg.role === 'assistant' ? marked.parse(msg.content) : msg.content" />
              </template>
            </div>
          </div>
        </div>

        <div class="shrink-0 border-t border-gray-100 bg-white p-4 lg:p-5">
          <div class="flex gap-2">
            <input
              v-model="input"
              :disabled="sending"
              class="h-11 flex-1 rounded-xl border border-gray-200 px-4 text-sm focus:border-amber-400 focus:outline-none focus:ring-2 focus:ring-amber-300 disabled:bg-gray-50"
              placeholder="输入您的问题..."
              @keyup.enter="send()"
            />
            <button
              :disabled="sending || !input.trim()"
              class="h-11 rounded-xl bg-amber-500 px-6 text-sm font-medium text-white transition-colors hover:bg-amber-600 disabled:bg-gray-300"
              @click="send()"
            >
              发送
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { marked } from 'marked'

definePageMeta({
  layout: 'blank',
})

type ChatMessage = {
  role: 'user' | 'assistant'
  content: string
  loading?: boolean
}

const messages = ref<ChatMessage[]>([])
const input = ref('')
const sending = ref(false)
const sessionId = ref('')
const msgList = ref<HTMLElement>()
const api = useApi()

const quickQuestions = ['如何退换货？', '优惠券怎么用？', '支付方式有哪些？', '订单多久发货？']

onMounted(() => {
  sessionId.value = localStorage.getItem('kb_session') || ''
})

function goBack() {
  if (import.meta.client && window.history.length > 1) {
    window.history.back()
    return
  }
  navigateTo('/')
}

function scrollBottom() {
  nextTick(() => {
    const el = msgList.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

async function send(quick?: string) {
  const text = quick || input.value.trim()
  if (!text || sending.value) {
    return
  }

  input.value = ''
  messages.value.push({ role: 'user', content: text })
  const aiMsg: ChatMessage = { role: 'assistant', content: '', loading: true }
  messages.value.push(aiMsg)
  scrollBottom()

  sending.value = true
  try {
    const res: any = await api.post('/knowledge/chat', {
      question: text,
      sessionId: sessionId.value || undefined,
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
    if (e?.status === 401) {
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
