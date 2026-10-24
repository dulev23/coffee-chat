package com.dulo.coffeechat;

import com.dulo.coffeechat.config.AppProperties;
import com.dulo.coffeechat.config.AppProperties.Gemini;
import com.dulo.coffeechat.model.Booking;
import com.dulo.coffeechat.model.TimeSlot;
import com.dulo.coffeechat.service.ConfirmationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ConfirmationServiceTest {

    private static final String FALLBACK = "Your coffee chat is booked, Alex! Dulo will see you on Monday, January 7 at 10:00 for 30 minutes. ☕";

    private final FakeGemini gemini = new FakeGemini();
    private final Booking booking = new Booking(
            new TimeSlot(LocalDateTime.of(2030, 1, 7, 10, 0), 30, LocalDateTime.now()),
            "Alex Morgan", "alex@example.com", "Spring Boot", LocalDateTime.now());

    @AfterEach
    void stop() {
        gemini.close();
    }

    @Test
    void returnsGeminiMessage() {
        gemini.respond(200, FakeGemini.textResponse("Hi Alex, see you Monday at 10:00 to talk Spring Boot! ☕"));

        assertThat(service("test-key", gemini.baseUrl()).generate(booking))
                .isEqualTo("Hi Alex, see you Monday at 10:00 to talk Spring Boot! ☕");
        assertThat(gemini.lastPath()).isEqualTo("/v1beta/interactions");
        assertThat(gemini.lastApiKey()).isEqualTo("test-key");
        assertThat(gemini.lastRequestBody())
                .contains("\"model\":\"gemini-3.5-flash-lite\"")
                .contains("\"system_instruction\"")
                .contains("\"store\":false")
                .contains("Alex Morgan")
                .doesNotContain("test-key");
    }

    @Test
    void stripsMarkdownFromGeminiMessage() {
        gemini.respond(200, FakeGemini.textResponse("**Hi Alex!** See you soon."));

        assertThat(service("test-key", gemini.baseUrl()).generate(booking)).isEqualTo("Hi Alex! See you soon.");
    }

    @Test
    void usesFallbackWhenGeminiReturnsServerError() {
        gemini.respond(500, "{\"error\": {\"message\": \"internal\"}}");

        assertThat(service("test-key", gemini.baseUrl()).generate(booking)).isEqualTo(FALLBACK);
    }

    @Test
    void usesFallbackWhenRateLimited() {
        gemini.respond(429, "{\"error\": {\"message\": \"quota exceeded\"}}");

        assertThat(service("test-key", gemini.baseUrl()).generate(booking)).isEqualTo(FALLBACK);
    }

    @Test
    void usesFallbackWhenResponseHasNoText() {
        gemini.respond(200, "{\"id\": \"int_123\", \"steps\": []}");

        assertThat(service("test-key", gemini.baseUrl()).generate(booking)).isEqualTo(FALLBACK);
    }

    @Test
    void usesFallbackWhenGeminiIsUnreachable() {
        assertThat(service("test-key", "http://localhost:1").generate(booking)).isEqualTo(FALLBACK);
    }

    @Test
    void usesFallbackWithoutApiKeyAndSkipsGemini() {
        assertThat(service("", gemini.baseUrl()).generate(booking)).isEqualTo(FALLBACK);
        assertThat(gemini.lastPath()).isNull();
    }

    private ConfirmationService service(String apiKey, String baseUrl) {
        var properties = new AppProperties("Dulo", "Europe/Skopje", List.of(),
                new Gemini(apiKey, "gemini-3.5-flash-lite", baseUrl));
        return new ConfirmationService(properties, RestClient.builder());
    }
}
