import type { Booking } from '../types'
import { dayKey, formatLongDay, formatTime } from '../utils/format'

interface Props {
  booking: Booking
  onDone: () => void
}

export default function Confirmation({ booking, onDone }: Props) {
  const { slot } = booking

  return (
    <div className="ticket-wrap" role="status">
      <article className="ticket">
        <div className="ticket-head">
          <span className="ticket-order">Order #{String(booking.id).padStart(3, '0')}</span>
          <span className="stamp">Booked</span>
        </div>
        <p className="ticket-when">{formatLongDay(dayKey(slot.startTime))}</p>
        <p className="ticket-time">
          {formatTime(slot.startTime)}–{formatTime(slot.endTime)}
        </p>
        <dl className="ticket-lines">
          <div>
            <dt>For</dt>
            <dd>{booking.name}</dd>
          </div>
          <div>
            <dt>With</dt>
            <dd>Dulo</dd>
          </div>
          <div>
            <dt>Length</dt>
            <dd>{slot.durationMinutes} min</dd>
          </div>
        </dl>
        <p className="ticket-note">{booking.message}</p>
      </article>
      <button className="ghost" onClick={onDone}>
        Back to times
      </button>
    </div>
  )
}
