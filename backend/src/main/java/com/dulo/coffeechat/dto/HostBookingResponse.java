package com.dulo.coffeechat.dto;

import com.dulo.coffeechat.model.Booking;

import java.time.LocalDateTime;

public record HostBookingResponse(Long id, SlotResponse slot, String name, String email, String topic,
                                  LocalDateTime createdAt) {

    public static HostBookingResponse from(Booking booking) {
        return new HostBookingResponse(booking.getId(), SlotResponse.from(booking.getTimeSlot()), booking.getName(),
                booking.getEmail(), booking.getTopic(), booking.getCreatedAt());
    }
}
