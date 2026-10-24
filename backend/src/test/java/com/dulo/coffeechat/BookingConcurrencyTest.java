package com.dulo.coffeechat;

import com.dulo.coffeechat.dto.CreateBookingRequest;
import com.dulo.coffeechat.model.TimeSlot;
import com.dulo.coffeechat.repository.BookingRepository;
import com.dulo.coffeechat.repository.TimeSlotRepository;
import com.dulo.coffeechat.service.BookingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BookingConcurrencyTest {

    private static final int ATTEMPTS = 8;

    @Autowired
    private BookingService bookingService;

    @Autowired
    private TimeSlotRepository slots;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private Clock clock;

    @BeforeEach
    void clean() {
        bookings.deleteAll();
        slots.deleteAll();
    }

    @Test
    void onlyOneOfConcurrentBookingsSucceeds() throws Exception {
        var now = LocalDateTime.now(clock);
        var slot = slots.save(new TimeSlot(now.plusDays(1), 30, now));
        var start = new CountDownLatch(1);
        var successes = new AtomicInteger();
        var conflicts = new AtomicInteger();

        try (var executor = Executors.newFixedThreadPool(ATTEMPTS)) {
            for (int i = 0; i < ATTEMPTS; i++) {
                var request = new CreateBookingRequest(slot.getId(), "Guest " + i, "guest" + i + "@example.com", null);
                executor.submit(() -> {
                    start.await();
                    try {
                        bookingService.book(request);
                        successes.incrementAndGet();
                    } catch (ResponseStatusException e) {
                        if (e.getStatusCode() == HttpStatus.CONFLICT) {
                            conflicts.incrementAndGet();
                        }
                    } catch (DataIntegrityViolationException e) {
                        conflicts.incrementAndGet();
                    }
                    return null;
                });
            }
            start.countDown();
        }

        assertThat(successes).hasValue(1);
        assertThat(conflicts).hasValue(ATTEMPTS - 1);
        assertThat(bookings.count()).isEqualTo(1);
        assertThat(slots.findById(slot.getId())).get().extracting(TimeSlot::isBooked).isEqualTo(true);
    }
}
