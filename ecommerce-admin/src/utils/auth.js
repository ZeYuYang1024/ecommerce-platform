function decodeBase64Url(value) {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/')
  const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, '=')
  const binary = atob(padded)
  const bytes = Uint8Array.from(binary, char => char.charCodeAt(0))
  return new TextDecoder().decode(bytes)
}

function extractIntegerClaim(payloadText, claimName) {
  const pattern = new RegExp(`"${claimName}"\\s*:\\s*(-?\\d+)`)
  const match = payloadText.match(pattern)
  return match ? match[1] : null
}

export function parseJwtPayload(token) {
  if (!token) return null

  const rawToken = token.startsWith('Bearer ') ? token.slice(7) : token
  const segments = rawToken.split('.')
  if (segments.length < 2) return null

  try {
    const payloadText = decodeBase64Url(segments[1])
    const payload = JSON.parse(payloadText)
    const merchantId = extractIntegerClaim(payloadText, 'merchantId')
    if (merchantId !== null) {
      payload.merchantId = merchantId
    }
    return payload
  } catch {
    return null
  }
}

export function getMerchantIdFromToken(token = localStorage.getItem('token')) {
  const merchantId = parseJwtPayload(token)?.merchantId
  return merchantId === null || merchantId === undefined ? null : String(merchantId)
}

export function ensureMerchantContext() {
  const userType = localStorage.getItem('type')
  const storedMerchantId = localStorage.getItem('merchantId')

  if (userType !== 'merchant') {
    if (storedMerchantId) localStorage.removeItem('merchantId')
    return null
  }

  const merchantIdFromToken = getMerchantIdFromToken()
  if (merchantIdFromToken) {
    if (storedMerchantId !== merchantIdFromToken) {
      localStorage.setItem('merchantId', merchantIdFromToken)
    }
    return merchantIdFromToken
  }

  if (storedMerchantId) return storedMerchantId

  return null
}

export function clearAuthContext() {
  localStorage.removeItem('token')
  localStorage.removeItem('username')
  localStorage.removeItem('type')
  localStorage.removeItem('merchantId')
}
