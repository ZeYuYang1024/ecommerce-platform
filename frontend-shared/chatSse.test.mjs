import test from 'node:test'
import assert from 'node:assert/strict'

import {
  createSseEventStreamParser,
  postJsonSse,
} from './chatSse.mjs'

test('createSseEventStreamParser should emit parsed events across split chunks', () => {
  const events = []
  const parser = createSseEventStreamParser((event) => events.push(event))

  parser.pushChunk('event:chunk\ndata:第一')
  parser.pushChunk('段\n\nevent:answer\ndata:{"answer":"ok"}\n\n')
  parser.flush()

  assert.deepEqual(events, [
    { event: 'chunk', data: '第一段' },
    { event: 'answer', data: { answer: 'ok' } },
  ])
})

test('createSseEventStreamParser should join multi-line data frames', () => {
  const events = []
  const parser = createSseEventStreamParser((event) => events.push(event))

  parser.pushChunk('event:chunk\ndata:第一行\ndata:第二行\n\n')

  assert.deepEqual(events, [
    { event: 'chunk', data: '第一行\n第二行' },
  ])
})

test('postJsonSse should throw status error for non-2xx responses', async () => {
  await assert.rejects(
    () =>
      postJsonSse({
        url: 'http://example.test/stream',
        payload: { question: 'test' },
        fetchImpl: async () => ({
          ok: false,
          status: 401,
        }),
      }),
    (error) => {
      assert.equal(error.message, 'HTTP 401')
      assert.equal(error.status, 401)
      return true
    }
  )
})

test('postJsonSse should throw when response body is empty', async () => {
  await assert.rejects(
    () =>
      postJsonSse({
        url: 'http://example.test/stream',
        payload: { question: 'test' },
        fetchImpl: async () => ({
          ok: true,
          status: 200,
          body: null,
        }),
      }),
    /Stream body is empty/
  )
})
