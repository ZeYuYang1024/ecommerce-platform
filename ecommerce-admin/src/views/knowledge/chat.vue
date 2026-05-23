<template>
  <div class="page-container">
    <div class="page-header">
      <div>
        <h2>{{ pageTitle }}</h2>
        <p class="page-desc">{{ pageDesc }}</p>
      </div>
      <el-tag v-if="routeName" effect="plain">{{ lightRouteName }} / {{ routeName }}</el-tag>
    </div>

    <div class="chat-layout">
      <section class="conversation-panel">
        <div ref="messageListRef" class="message-list">
          <el-empty v-if="messages.length === 0" description="输入问题后开始调试知识问答链路" />
          <div
            v-for="(message, index) in messages"
            :key="index"
            class="message-row"
            :class="{ user: message.role === 'user' }"
          >
            <div class="message-bubble" :class="message.role">
              <div v-if="message.loading" class="loading-text">正在生成回复...</div>
              <div v-else class="message-content">{{ message.content }}</div>
            </div>
          </div>
        </div>

        <div class="input-row">
          <el-input
            v-model="question"
            :disabled="streaming"
            placeholder="输入问题或调试语句"
            clearable
            @keyup.enter="send"
          />
          <el-button type="primary" :loading="streaming" :disabled="!question.trim()" @click="send">
            发送
          </el-button>
        </div>
      </section>

      <aside class="event-panel">
        <div class="panel-title">事件时间线</div>
        <div v-if="events.length === 0" class="empty-events">暂无事件</div>
        <div v-for="(item, index) in events" :key="index" class="event-item">
          <span class="event-name">{{ item.label }}</span>
          <span class="event-data">{{ item.data }}</span>
        </div>
      </aside>
    </div>
  </div>
</template>

<script setup>
import { computed, nextTick, ref } from 'vue'
import { useRoute } from 'vue-router'
import { postChatStream } from '@/utils/chatStream'

const route = useRoute()
const isMerchantView = computed(() => route.path.startsWith('/merchant/knowledge'))
const pageTitle = computed(() => (isMerchantView.value ? '商家知识问答工作台' : '知识问答工作台'))
const pageDesc = computed(() => (isMerchantView.value
  ? '调试商家知识库问答、路由和流式返回'
  : '调试平台知识库问答、路由和流式返回'))
const streamUrl = computed(() => (isMerchantView.value
  ? '/api/v1/admin/merchant/knowledge/chat/stream'
  : '/api/v1/admin/knowledge/chat/stream'))

const question = ref('')
const sessionId = ref('')
const streaming = ref(false)
const messages = ref([])
const events = ref([])
const lightRouteName = ref('-')
const routeName = ref('')
const messageListRef = ref(null)
const eventLabelMap = {
  start: '会话开始',
  lightRoute: '轻量路由',
  route: '业务路由',
  chunk: '分段内容',
  answer: '最终答案',
  done: '会话完成',
  error: '错误'
}

function scrollBottom() {
  nextTick(() => {
    const el = messageListRef.value
    if (el) {
      el.scrollTop = el.scrollHeight
    }
  })
}

function recordEvent(event, data) {
  const displayData = (() => {
    if (event === 'answer' && typeof data === 'object' && data) {
      return data.answer || '暂无回答'
    }
    if (typeof data === 'string') {
      return data
    }
    return JSON.stringify(data)
  })()

  events.value.push({
    event,
    label: eventLabelMap[event] || event,
    data: displayData
  })
}

async function send() {
  const text = question.value.trim()
  if (!text || streaming.value) return

  question.value = ''
  events.value = []
  lightRouteName.value = '-'
  routeName.value = ''
  messages.value.push({ role: 'user', content: text })
  const assistant = { role: 'assistant', content: '', loading: true }
  messages.value.push(assistant)
  streaming.value = true
  scrollBottom()

  try {
    await postChatStream(
      streamUrl.value,
      {
        question: text,
        sessionId: sessionId.value || undefined
      },
      ({ event, data }) => {
        recordEvent(event, data)
        if (event === 'start' && typeof data === 'string') {
          sessionId.value = data
          return
        }
        if (event === 'lightRoute') {
          lightRouteName.value = String(data || '-')
          return
        }
        if (event === 'route') {
          routeName.value = String(data || '')
          return
        }
        if (event === 'chunk') {
          assistant.loading = false
          assistant.content += typeof data === 'string' ? data : String(data ?? '')
          scrollBottom()
          return
        }
        if (event === 'answer') {
          assistant.loading = false
          const answer = typeof data === 'object' && data ? data.answer : data
          assistant.content = answer || assistant.content || '暂无回答'
          if (typeof data === 'object' && data?.sessionId) {
            sessionId.value = data.sessionId
          }
          scrollBottom()
          return
        }
        if (event === 'error') {
          assistant.loading = false
          assistant.content = typeof data === 'string' ? data : '问答服务暂时不可用'
          scrollBottom()
        }
      }
    )
    assistant.loading = false
    assistant.content = assistant.content || '暂无回答'
  } catch (error) {
    assistant.loading = false
    assistant.content = error?.status === 401 ? '登录已失效，请重新登录' : '问答服务暂时不可用'
  } finally {
    streaming.value = false
    scrollBottom()
  }
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  margin-bottom: 20px;
}

.page-header h2 {
  margin: 0;
  font-size: 20px;
}

.page-desc {
  margin: 6px 0 0;
  color: var(--text-muted);
  font-size: 13.5px;
}

.chat-layout {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 320px;
  gap: 16px;
  min-height: calc(100vh - 170px);
}

.conversation-panel,
.event-panel {
  border: 1px solid var(--border-subtle);
  border-radius: 8px;
  background: var(--bg-card);
}

.conversation-panel {
  display: flex;
  min-width: 0;
  flex-direction: column;
}

.message-list {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  padding: 20px;
}

.message-row {
  display: flex;
  margin-bottom: 12px;
}

.message-row.user {
  justify-content: flex-end;
}

.message-bubble {
  max-width: 72%;
  border-radius: 8px;
  padding: 10px 12px;
  line-height: 1.6;
  font-size: 14px;
  white-space: pre-wrap;
}

.message-bubble.user {
  background: var(--accent);
  color: #1a1816;
}

.message-bubble.assistant {
  background: #f8fafc;
  color: var(--text-primary);
  border: 1px solid var(--border-subtle);
}

.loading-text,
.empty-events {
  color: var(--text-muted);
  font-size: 13px;
}

.input-row {
  display: flex;
  gap: 10px;
  padding: 16px;
  border-top: 1px solid var(--border-subtle);
}

.event-panel {
  padding: 16px;
  overflow-y: auto;
}

.panel-title {
  margin-bottom: 12px;
  font-weight: 600;
  color: var(--text-primary);
}

.event-item {
  display: grid;
  grid-template-columns: 96px minmax(0, 1fr);
  gap: 8px;
  padding: 8px 0;
  border-bottom: 1px solid var(--border-subtle);
  font-size: 12px;
}

.event-name {
  font-weight: 600;
  color: var(--accent);
}

.event-data {
  color: var(--text-secondary);
  word-break: break-word;
}

@media (max-width: 1100px) {
  .chat-layout {
    grid-template-columns: 1fr;
  }

  .event-panel {
    max-height: 260px;
  }
}
</style>
