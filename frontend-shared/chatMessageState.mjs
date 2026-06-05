export function createStreamingAssistantMessage(messagesRef) {
  messagesRef.value.push({
    role: 'assistant',
    content: '',
    loading: true,
  })

  const messageIndex = messagesRef.value.length - 1

  function getMessage() {
    return messagesRef.value[messageIndex]
  }

  return {
    get role() {
      return getMessage()?.role
    },
    get content() {
      return getMessage()?.content ?? ''
    },
    set content(value) {
      const message = getMessage()
      if (message) {
        message.content = value
      }
    },
    get loading() {
      return getMessage()?.loading ?? false
    },
    set loading(value) {
      const message = getMessage()
      if (message) {
        message.loading = value
      }
    },
  }
}
