const time = new Intl.DateTimeFormat('en-GB', { hour: '2-digit', minute: '2-digit' })
const weekday = new Intl.DateTimeFormat('en-GB', { weekday: 'short' })
const longDay = new Intl.DateTimeFormat('en-GB', { weekday: 'long', day: 'numeric', month: 'long' })

export const formatTime = (iso: string) => time.format(new Date(iso))
const localDay = (day: string) => new Date(`${day}T00:00`)

export const formatWeekday = (day: string) => weekday.format(localDay(day))
export const formatDayNumber = (day: string) => localDay(day).getDate()
export const formatLongDay = (day: string) => longDay.format(localDay(day))

export const dayKey = (iso: string) => iso.slice(0, 10)

export function groupByDay<T extends { startTime: string }>(items: T[]): [string, T[]][] {
  const groups = new Map<string, T[]>()
  for (const item of items) {
    const key = dayKey(item.startTime)
    groups.set(key, [...(groups.get(key) ?? []), item])
  }
  return [...groups.entries()]
}

export function todayKey() {
  const now = new Date()
  return new Date(now.getTime() - now.getTimezoneOffset() * 60_000).toISOString().slice(0, 10)
}
