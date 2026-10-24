import { useState, type FormEvent } from 'react'
import { useApi } from '../hooks/useApi'
import { api } from '../services/api'
import type { HostBooking, Slot } from '../types'
import { formatLongDay, formatTime, groupByDay, todayKey } from '../utils/format'

const DURATIONS = [15, 30, 45, 60]

interface Row {
  startTime: string
  slot: Slot
  booking: HostBooking | null
}

const loadSchedule = () => Promise.all([api.slots(), api.bookings()])

function toRows([slots, bookings]: Awaited<ReturnType<typeof loadSchedule>>): Row[] {
  return [
    ...slots.map((slot) => ({ startTime: slot.startTime, slot, booking: null })),
    ...bookings.map((booking) => ({ startTime: booking.slot.startTime, slot: booking.slot, booking })),
  ].sort((a, b) => a.startTime.localeCompare(b.startTime))
}

export default function HostPage() {
  const { data, error: loadError, reload } = useApi(loadSchedule, "Couldn't load your schedule. Is the API running?")
  const [duration, setDuration] = useState(30)
  const [actionError, setActionError] = useState<string | null>(null)
  const [saving, setSaving] = useState(false)

  const rows = data ? toRows(data) : null
  const bookedCount = data?.[1].length ?? 0
  const error = actionError ?? loadError

  async function handleCreate(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    const form = event.currentTarget
    const formData = new FormData(form)
    setSaving(true)
    setActionError(null)
    try {
      await api.createSlot({
        startTime: `${formData.get('date')}T${formData.get('startTime')}:00`,
        durationMinutes: duration,
      })
      form.reset()
      await reload()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Could not add that time.')
    } finally {
      setSaving(false)
    }
  }

  async function handleDelete(id: number) {
    setActionError(null)
    try {
      await api.deleteSlot(id)
      await reload()
    } catch (e) {
      setActionError(e instanceof Error ? e.message : 'Could not remove that time.')
    }
  }

  return (
    <div className="host-layout">
      <section className="host-add">
        <h1>Your open times</h1>
        <p className="muted">
          {rows ? `${rows.length} upcoming, ${bookedCount} booked.` : 'Add times people can book with you.'}
        </p>

        <form className="add-form" onSubmit={handleCreate}>
          <label>
            Date
            <input name="date" type="date" required min={todayKey()} />
          </label>
          <label>
            Start time
            <input name="startTime" type="time" required step={300} />
          </label>
          <fieldset>
            <legend>Length</legend>
            <div className="segmented">
              {DURATIONS.map((minutes) => (
                <button
                  type="button"
                  key={minutes}
                  aria-pressed={duration === minutes}
                  onClick={() => setDuration(minutes)}
                >
                  {minutes} min
                </button>
              ))}
            </div>
          </fieldset>
          <button type="submit" className="primary" disabled={saving}>
            {saving ? 'Adding…' : 'Add time'}
          </button>
        </form>
        {error && (
          <p className="notice error" role="alert">
            {error}
          </p>
        )}
        <p className="muted">This page has no login on purpose: authentication is outside the demo's scope.</p>
      </section>

      <section className="host-list" aria-label="Upcoming times">
        {rows?.length === 0 && (
          <div className="empty">
            <h2>No times yet</h2>
            <p className="muted">Add your first time and it shows up on your booking page.</p>
          </div>
        )}
        {groupByDay(rows ?? []).map(([day, items]) => (
          <div key={day} className="host-day">
            <h2>{formatLongDay(day)}</h2>
            <ul>
              {items.map(({ slot, booking }) => (
                <li key={slot.id} className={booking ? 'row booked' : 'row'}>
                  <span className="row-time">
                    {formatTime(slot.startTime)}–{formatTime(slot.endTime)}
                  </span>
                  {booking ? (
                    <span className="row-guest">
                      <strong>{booking.name}</strong>
                      <a href={`mailto:${booking.email}`}>{booking.email}</a>
                      {booking.topic && <span className="muted">“{booking.topic}”</span>}
                    </span>
                  ) : (
                    <span className="row-guest muted">Open</span>
                  )}
                  {!booking && (
                    <button className="ghost small" onClick={() => handleDelete(slot.id)}>
                      Remove
                    </button>
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </section>
    </div>
  )
}
