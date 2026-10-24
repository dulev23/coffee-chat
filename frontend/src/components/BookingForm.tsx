import { useState, type FormEvent } from 'react'
import { api, ApiError } from '../services/api'
import type { Booking, Slot } from '../types'
import { dayKey, formatLongDay, formatTime } from '../utils/format'

interface Props {
  slot: Slot
  onBooked: (booking: Booking) => void
  onTaken: (message: string) => void
  onCancel: () => void
}

export default function BookingForm({ slot, onBooked, onTaken, onCancel }: Props) {
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState<string | null>(null)

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault()
    if (submitting) return

    const data = new FormData(event.currentTarget)
    const name = String(data.get('name')).trim()
    if (!name) {
      setError('Please enter your name.')
      return
    }

    setSubmitting(true)
    setError(null)
    try {
      onBooked(
        await api.book({
          slotId: slot.id,
          name,
          email: String(data.get('email')).trim(),
          topic: String(data.get('topic')),
        }),
      )
    } catch (e) {
      if (e instanceof ApiError && e.status === 409) {
        onTaken(e.message)
        return
      }
      setError(e instanceof Error ? e.message : 'Booking failed. Try again.')
      setSubmitting(false)
    }
  }

  return (
    <form className="booking-form" onSubmit={handleSubmit} aria-busy={submitting}>
      <p className="form-title">
        {formatLongDay(dayKey(slot.startTime))}, {formatTime(slot.startTime)}–{formatTime(slot.endTime)}
      </p>
      <label>
        Your name
        <input name="name" required maxLength={100} autoComplete="name" autoFocus />
      </label>
      <label>
        Email
        <input name="email" type="email" required maxLength={255} autoComplete="email" />
      </label>
      <label>
        What would you like to talk about? <span className="muted">Optional</span>
        <textarea name="topic" maxLength={280} rows={2} placeholder="e.g. how you got into DevOps" />
      </label>
      {error && (
        <p className="notice error" role="alert">
          {error}
        </p>
      )}
      <div className="form-actions">
        <button type="submit" className="primary" disabled={submitting}>
          {submitting ? 'Booking…' : 'Book this time'}
        </button>
        <button type="button" className="ghost" onClick={onCancel} disabled={submitting}>
          Cancel
        </button>
      </div>
    </form>
  )
}
