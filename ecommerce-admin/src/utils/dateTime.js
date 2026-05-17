export function formatDateTime(value) {
  if (!value) return '-'
  return String(value).replace('T', ' ')
}
