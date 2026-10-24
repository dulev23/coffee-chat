export interface Slot {
  id: number
  startTime: string
  endTime: string
  durationMinutes: number
  booked: boolean
}

export interface Booking {
  id: number
  slot: Slot
  name: string
  message: string
}

export interface HostBooking {
  id: number
  slot: Slot
  name: string
  email: string
  topic: string | null
  createdAt: string
}

export interface NewSlot {
  startTime: string
  durationMinutes: number
}

export interface BookingRequest {
  slotId: number
  name: string
  email: string
  topic: string
}
