const pad = (n: number, width = 2) => String(n).padStart(width, '0')

/** Local clock time with milliseconds, e.g. 17:42:21.347 — lines in the same second stay ordered. */
export function formatClock(iso: string): string {
  const d = new Date(iso)
  return `${pad(d.getHours())}:${pad(d.getMinutes())}:${pad(d.getSeconds())}.${pad(d.getMilliseconds(), 3)}`
}

/** Full date, time and timezone for tooltips, e.g. "1 Oct 2026 at 17:42:21 GMT+5:30". */
export function formatFull(iso: string): string {
  return new Date(iso).toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'long' })
}

/** Compact relative time, e.g. "3s ago", "12 min ago", "2 h ago". */
export function timeAgo(iso: string | null, now = Date.now()): string {
  if (!iso) {
    return '—'
  }
  const seconds = Math.max(0, Math.round((now - new Date(iso).getTime()) / 1000))
  if (seconds < 60) {
    return `${seconds}s ago`
  }
  if (seconds < 3600) {
    return `${Math.round(seconds / 60)} min ago`
  }
  if (seconds < 86400) {
    return `${Math.round(seconds / 3600)} h ago`
  }
  return `${Math.round(seconds / 86400)} d ago`
}
