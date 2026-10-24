package com.dulo.coffeechat.dto;

import com.dulo.coffeechat.model.TimeSlot;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public record CreateSlotRequest(
        @NotNull LocalDateTime startTime,
        @NotNull @Min(TimeSlot.MIN_DURATION_MINUTES) @Max(TimeSlot.MAX_DURATION_MINUTES) Integer durationMinutes) {
}
