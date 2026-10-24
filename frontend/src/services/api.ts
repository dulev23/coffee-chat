import type { Booking, BookingRequest, HostBooking, NewSlot, Slot } from '../types'

const BASE_URL = (import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number

  constructor(message: string, status: number) {
    super(message)
    this.status = status
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  const response = await fetch(`${BASE_URL}/api${path}`, {
    ...init,
    headers: { 'Content-Type': 'application/json', ...init?.headers },
  })
  if (!response.ok) {
    const problem = await response.json().catch(() => null)
    throw new ApiError(problem?.detail ?? 'Something went wrong. Try again.', response.status)
  }
  return response.status === 204 ? (undefined as T) : response.json()
}

export const api = {
  slots: () => request<Slot[]>('/slots'),
  createSlot: (body: NewSlot) => request<Slot>('/slots', { method: 'POST', body: JSON.stringify(body) }),
  deleteSlot: (id: number) => request<void>(`/slots/${id}`, { method: 'DELETE' }),
  bookings: () => request<HostBooking[]>('/bookings'),
  book: (body: BookingRequest) => request<Booking>('/bookings', { method: 'POST', body: JSON.stringify(body) }),
}
