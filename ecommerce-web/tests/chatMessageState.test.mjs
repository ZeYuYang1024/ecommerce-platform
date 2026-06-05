import test from 'node:test'
import assert from 'node:assert/strict'
import { nextTick, ref, watchEffect } from 'vue'

import { createStreamingAssistantMessage } from '../../frontend-shared/chatMessageState.mjs'

test('createStreamingAssistantMessage should update content reactively while streaming', async () => {
  const messages = ref([])
  let rendered = null

  watchEffect(() => {
    const message = messages.value[0]
    rendered = message
      ? { content: message.content, loading: message.loading }
      : null
  })

  const assistant = createStreamingAssistantMessage(messages)
  await nextTick()

  assistant.loading = false
  assistant.content += 'part-1'
  await nextTick()
  assert.deepEqual(rendered, { content: 'part-1', loading: false })

  assistant.content += 'part-2'
  await nextTick()
  assert.deepEqual(rendered, { content: 'part-1part-2', loading: false })

  assistant.content = 'final-answer'
  await nextTick()
  assert.deepEqual(rendered, { content: 'final-answer', loading: false })
})
