package com.dulo.coffeechat;

import com.dulo.coffeechat.model.TimeSlot;
import com.dulo.coffeechat.repository.BookingRepository;
import com.dulo.coffeechat.repository.TimeSlotRepository;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GeminiBookingFlowTest {

    private static final FakeGemini GEMINI = new FakeGemini();

    @DynamicPropertySource
    static void geminiProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:h2:mem:gemini-flow;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        registry.add("app.gemini.api-key", () -> "test-key");
        registry.add("app.gemini.base-url", GEMINI::baseUrl);
    }

    @Autowired
    private MockMvc mvc;

    @Autowired
    private TimeSlotRepository slots;

    @Autowired
    private BookingRepository bookings;

    @Autowired
    private Clock clock;

    @AfterAll
    static void stopGemini() {
        GEMINI.close();
    }

    @BeforeEach
    void clean() {
        bookings.deleteAll();
        slots.deleteAll();
    }

    @Test
    void returnsGeminiConfirmationInBookingResponse() throws Exception {
        GEMINI.respond(200, FakeGemini.textResponse("Hi Alex, your coffee chat is set. See you soon! ☕"));
        var slot = saveSlot();

        book(slot.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Hi Alex, your coffee chat is set. See you soon! ☕"));
    }

    @Test
    void bookingSucceedsWithFallbackWhenGeminiFails() throws Exception {
        GEMINI.respond(503, "{\"error\": {\"message\": \"unavailable\"}}");
        var slot = saveSlot();

        book(slot.getId())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value(startsWith("Your coffee chat is booked, Alex!")));
        mvc.perform(get("/api/slots")).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/bookings")).andExpect(jsonPath("$", hasSize(1)));
    }

    private TimeSlot saveSlot() {
        var now = LocalDateTime.now(clock).truncatedTo(ChronoUnit.MINUTES);
        return slots.save(new TimeSlot(now.plusDays(1), 30, now));
    }

    private ResultActions book(Long slotId) throws Exception {
        return mvc.perform(post("/api/bookings").contentType(APPLICATION_JSON).content("""
                {"slotId": %d, "name": "Alex Morgan", "email": "alex@example.com"}
                """.formatted(slotId)));
    }
}
