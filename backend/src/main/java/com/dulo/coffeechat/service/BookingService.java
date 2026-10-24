package com.dulo.coffeechat.service;

import com.dulo.coffeechat.dto.CreateBookingRequest;
import com.dulo.coffeechat.model.Booking;
import com.dulo.coffeechat.repository.BookingRepository;
import com.dulo.coffeechat.repository.TimeSlotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class BookingService {

    private final BookingRepository bookings;
    private final TimeSlotRepository slots;
    private final Clock clock;

    public BookingService(BookingRepository bookings, TimeSlotRepository slots, Clock clock) {
        this.bookings = bookings;
        this.slots = slots;
        this.clock = clock;
    }

    @Transactional
    public Booking book(CreateBookingRequest request) {
        var slot = slots.findByIdForUpdate(request.slotId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Slot not found."));
        var now = LocalDateTime.now(clock);

        if (slot.isBooked()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Someone just booked this time. Please pick another one.");
        }
        if (!slot.getStartTime().isAfter(now)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This slot has already started.");
        }
        slot.markBooked();
        var topic = StringUtils.hasText(request.topic()) ? request.topic().trim() : null;
        return bookings.saveAndFlush(
                new Booking(slot, request.name().trim(), request.email().trim(), topic, now));
    }

    @Transactional(readOnly = true)
    public List<Booking> findUpcoming() {
        return bookings.findUpcoming(LocalDateTime.now(clock));
    }
}
