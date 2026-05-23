function parseEventData(data) {
  const trimmed = data.trim()
  if (!trimmed) {
    return ''
  }

  try {
    return JSON.parse(trimmed)
  } catch {
    return trimmed
  }
}

function parseSseFrame(frame) {
  let event = 'message'
  const dataLines = []

  for (const line of frame.split(/\r?\n/)) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart())
    }
  }

  if (!dataLines.length) {
    return null
  }

  return {
    event,
    data: parseEventData(dataLines.join('\n')),
  }
}

export function createSseEventStreamParser(onEvent) {
  let buffer = ''

  return {
    pushChunk(chunk) {
      buffer += chunk
      const frames = buffer.split(/\r?\n\r?\n/)
      buffer = frames.pop() || ''

      for (const frame of frames) {
        const parsed = parseSseFrame(frame)
        if (parsed) {
          onEvent(parsed)
        }
      }
    },
    flush() {
      const parsed = parseSseFrame(buffer)
      if (parsed) {
        onEvent(parsed)
      }
      buffer = ''
    },
  }
}

export async function postJsonSse({
  url,
  payload,
  headers = {},
  fetchImpl = fetch,
  onEvent,
}) {
  const response = await fetchImpl(url, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Accept: 'text/event-stream',
      ...headers,
    },
    body: JSON.stringify(payload),
  })

  if (!response.ok) {
    const error = new Error(`HTTP ${response.status}`)
    error.status = response.status
    throw error
  }

  if (!response.body) {
    throw new Error('Stream body is empty')
  }

  const reader = response.body.getReader()
  const decoder = new TextDecoder('utf-8')
  const parser = createSseEventStreamParser(onEvent)

  while (true) {
    const { value, done } = await reader.read()
    if (done) {
      break
    }

    parser.pushChunk(decoder.decode(value, { stream: true }))
  }

  parser.pushChunk(decoder.decode())
  parser.flush()
}
