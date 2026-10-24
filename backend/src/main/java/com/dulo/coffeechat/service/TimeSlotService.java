package com.dulo.coffeechat.service;

import com.dulo.coffeechat.dto.CreateSlotRequest;
import com.dulo.coffeechat.model.TimeSlot;
import com.dulo.coffeechat.repository.TimeSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class TimeSlotService {

    private final TimeSlotRepository slots;
    private final Clock clock;

    public TimeSlotService(TimeSlotRepository slots, Clock clock) {
        this.slots = slots;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<TimeSlot> findAvailable() {
        return slots.findByBookedFalseAndStartTimeAfterOrderByStartTime(LocalDateTime.now(clock));
    }

    @Transactional(readOnly = true)
    public TimeSlot findById(Long id) {
        return slots.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
    }

    @Transactional
    public TimeSlot create(CreateSlotRequest request) {
        var start = request.startTime();
        var end = start.plusMinutes(request.durationMinutes());
        var now = LocalDateTime.now(clock);

        if (!start.isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Slots must start in the future.");
        }
        var overlaps = slots.findByStartTimeBetween(start.minusMinutes(TimeSlot.MAX_DURATION_MINUTES), end).stream()
                .anyMatch(slot -> slot.overlaps(start, end));
        if (overlaps) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This overlaps with another slot.");
        }
        return slots.save(new TimeSlot(start, request.durationMinutes(), now));
    }

    @Transactional
    public void delete(Long id) {
        var slot = findById(id);
        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booked slots can't be removed.");
        }
        slots.delete(slot);
    }
}
