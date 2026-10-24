package com.dulo.coffeechat.dto;

import com.dulo.coffeechat.model.Booking;

public record BookingResponse(Long id, SlotResponse slot, String name, String message) {

    public static BookingResponse from(Booking booking, String message) {
        return new BookingResponse(booking.getId(), SlotResponse.from(booking.getTimeSlot()), booking.getName(),
                message);
    }
}
