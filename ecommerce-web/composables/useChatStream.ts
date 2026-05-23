import { postJsonSse } from '../../frontend-shared/chatSse.mjs'

type ChatStreamEvent = {
  event: string
  data: any
}

type ChatStreamPayload = {
  question: string
  sessionId?: string
}

type ChatStreamHandlers = {
  onEvent?: (event: ChatStreamEvent) => void
}

export const useChatStream = () => {
  const config = useRuntimeConfig()
  const base = config.public.apiBase as string
  const token = useCookie('token')

  async function streamKnowledgeChat(payload: ChatStreamPayload, handlers: ChatStreamHandlers = {}) {
    const headers: Record<string, string> = {
    }
    if (token.value) {
      headers.Authorization = `Bearer ${token.value}`
    }

    await postJsonSse({
      url: `${base}/knowledge/chat/stream`,
      payload,
      headers,
      onEvent: (event) => handlers.onEvent?.(event as ChatStreamEvent),
    })
  }

  return {
    streamKnowledgeChat,
  }
}
