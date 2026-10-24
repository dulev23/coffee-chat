package com.dulo.coffeechat.dto;

import com.dulo.coffeechat.model.TimeSlot;

import java.time.LocalDateTime;

public record SlotResponse(Long id, LocalDateTime startTime, LocalDateTime endTime, int durationMinutes,
                           boolean booked) {

    public static SlotResponse from(TimeSlot slot) {
        return new SlotResponse(slot.getId(), slot.getStartTime(), slot.getEndTime(), slot.getDurationMinutes(),
                slot.isBooked());
    }
}
