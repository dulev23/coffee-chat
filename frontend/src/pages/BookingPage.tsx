import { useEffect, useMemo, useState } from 'react'
import BookingForm from '../components/BookingForm'
import Confirmation from '../components/Confirmation'
import { useApi } from '../hooks/useApi'
import { api } from '../services/api'
import type { Booking, Slot } from '../types'
import { formatDayNumber, formatLongDay, formatTime, formatWeekday, groupByDay } from '../utils/format'

export default function BookingPage() {
  const { data: slots, error, reload } = useApi(api.slots, "Couldn't load times. Check your connection and refresh.")
  const [notice, setNotice] = useState<string | null>(null)
  const [day, setDay] = useState<string | null>(null)
  const [selected, setSelected] = useState<Slot | null>(null)
  const [booking, setBooking] = useState<Booking | null>(null)
  const [slow, setSlow] = useState(false)

  useEffect(() => {
    const timer = setTimeout(() => setSlow(true), 3000)
    return () => clearTimeout(timer)
  }, [])

  const days = useMemo(() => groupByDay(slots ?? []), [slots])
  const activeDay = days.some(([key]) => key === day) ? day : (days[0]?.[0] ?? null)
  const daySlots = days.find(([key]) => key === activeDay)?.[1] ?? []

  const onBooked = (result: Booking) => {
    setBooking(result)
    setNotice(null)
    setSelected(null)
    reload()
  }

  const onTaken = (message: string) => {
    setNotice(message)
    setSelected(null)
    reload()
  }

  return (
    <div className="booking-layout">
      <section className="intro">
        <h1>Let's grab a coffee.</h1>
        <p>
          Pick a time below for a relaxed chat about software, careers or whatever you're building. Leave your name
          and email and the time is yours.
        </p>
        <p className="muted">All times are Skopje time (CET).</p>
      </section>

      <section className="picker" aria-label="Available times">
        {booking ? (
          <Confirmation booking={booking} onDone={() => setBooking(null)} />
        ) : (
          <>
            {error && (
              <p className="notice error" role="alert">
                {error}
              </p>
            )}
            {notice && (
              <p className="notice" role="status">
                {notice}
              </p>
            )}
            {slots === null && !error && (
              <p className="muted" role="status">
                {slow ? 'Waking up the server. On the free tier this can take up to a minute.' : 'Loading times…'}
              </p>
            )}
            {slots?.length === 0 && (
              <div className="empty">
                <h2>No open times right now</h2>
                <p className="muted">New times get added every week. Check back soon.</p>
              </div>
            )}

            {days.length > 0 && (
              <>
                <div className="days" role="tablist" aria-label="Days">
                  {days.map(([key, items]) => (
                    <button
                      key={key}
                      role="tab"
                      aria-selected={key === activeDay}
                      className="day"
                      onClick={() => {
                        setDay(key)
                        setSelected(null)
                      }}
                    >
                      <span className="day-name">{formatWeekday(key)}</span>
                      <span className="day-number">{formatDayNumber(key)}</span>
                      <span className="day-count">{items.length} open</span>
                    </button>
                  ))}
                </div>

                {activeDay && <h2 className="day-title">{formatLongDay(activeDay)}</h2>}
                <ul className="times">
                  {daySlots.map((slot) => (
                    <li key={slot.id}>
                      <button
                        className="time"
                        aria-pressed={selected?.id === slot.id}
                        aria-label={`Book ${formatTime(slot.startTime)}, ${slot.durationMinutes} minutes`}
                        onClick={() => {
                          setSelected(slot)
                          setNotice(null)
                        }}
                      >
                        <span>{formatTime(slot.startTime)}</span>
                        <span className="time-meta">{slot.durationMinutes} min</span>
                      </button>
                    </li>
                  ))}
                </ul>

                {selected && (
                  <BookingForm
                    key={selected.id}
                    slot={selected}
                    onBooked={onBooked}
                    onTaken={onTaken}
                    onCancel={() => setSelected(null)}
                  />
                )}
              </>
            )}
          </>
        )}
      </section>
    </div>
  )
}
