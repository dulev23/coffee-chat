package com.dulo.coffeechat;

import com.dulo.coffeechat.model.TimeSlot;
import com.dulo.coffeechat.repository.BookingRepository;
import com.dulo.coffeechat.repository.TimeSlotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.emptyString;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SchedulingApiTest {

    @Autowired
    private MockMvc mvc;

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
    void createsValidSlot() throws Exception {
        createSlot(tomorrow(), 30)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.durationMinutes").value(30))
                .andExpect(jsonPath("$.booked").value(false));
    }

    @Test
    void rejectsSlotInThePast() throws Exception {
        createSlot(now().minusHours(1), 30).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsInvalidDuration() throws Exception {
        createSlot(tomorrow(), 5).andExpect(status().isBadRequest());
        createSlot(tomorrow(), 500).andExpect(status().isBadRequest());
    }

    @Test
    void rejectsOverlappingSlot() throws Exception {
        saveSlot(tomorrow());
        createSlot(tomorrow().plusMinutes(15), 30).andExpect(status().isConflict());
    }

    @Test
    void listsOnlyAvailableFutureSlotsInOrder() throws Exception {
        var later = saveSlot(tomorrow().plusHours(3));
        var earlier = saveSlot(tomorrow());
        var taken = new TimeSlot(tomorrow().plusHours(5), 30, now());
        taken.markBooked();
        slots.save(taken);

        mvc.perform(get("/api/slots"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(earlier.getId()))
                .andExpect(jsonPath("$[1].id").value(later.getId()));
    }

    @Test
    void createsBookingAndRemovesSlotFromAvailability() throws Exception {
        var slot = saveSlot(tomorrow());

        book(slot.getId(), "Alex Morgan", "alex@example.com")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Alex Morgan"))
                .andExpect(jsonPath("$.slot.id").value(slot.getId()))
                .andExpect(jsonPath("$.slot.booked").value(true))
                .andExpect(jsonPath("$.message").value(not(emptyString())));

        mvc.perform(get("/api/slots")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/bookings"))
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("alex@example.com"));
    }

    @Test
    void rejectsInvalidBookingDetails() throws Exception {
        var slot = saveSlot(tomorrow());

        book(slot.getId(), " ", "alex@example.com").andExpect(status().isBadRequest());
        book(slot.getId(), "Alex Morgan", "not-an-email").andExpect(status().isBadRequest());
        mvc.perform(get("/api/slots")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void rejectsDuplicateBookingWithConflict() throws Exception {
        var slot = saveSlot(tomorrow());

        book(slot.getId(), "Alex Morgan", "alex@example.com").andExpect(status().isCreated());
        book(slot.getId(), "Sam Lee", "sam@example.com")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").isNotEmpty());
    }

    @Test
    void returnsNotFoundForUnknownSlot() throws Exception {
        book(999_999L, "Alex Morgan", "alex@example.com").andExpect(status().isNotFound());
        mvc.perform(get("/api/slots/999999")).andExpect(status().isNotFound());
    }

    private ResultActions createSlot(LocalDateTime startTime, int durationMinutes) throws Exception {
        return mvc.perform(post("/api/slots").contentType(APPLICATION_JSON).content("""
                {"startTime": "%s", "durationMinutes": %d}
                """.formatted(startTime, durationMinutes)));
    }

    private ResultActions book(Long slotId, String name, String email) throws Exception {
        return mvc.perform(post("/api/bookings").contentType(APPLICATION_JSON).content("""
                {"slotId": %d, "name": "%s", "email": "%s"}
                """.formatted(slotId, name, email)));
    }

    private TimeSlot saveSlot(LocalDateTime startTime) {
        return slots.save(new TimeSlot(startTime, 30, now()));
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
    }

    private LocalDateTime tomorrow() {
        return now().plusDays(1);
    }
}
