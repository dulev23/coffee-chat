package com.dulo.coffeechat;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

final class FakeGemini implements AutoCloseable {

    private final HttpServer server;
    private volatile int status = 200;
    private volatile String responseBody = "{}";
    private volatile String lastPath;
    private volatile String lastApiKey;
    private volatile String lastRequestBody;

    FakeGemini() {
        try {
            server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        server.createContext("/", exchange -> {
            lastPath = exchange.getRequestURI().getPath();
            lastApiKey = exchange.getRequestHeaders().getFirst("x-goog-api-key");
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            var bytes = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
    }

    static String textResponse(String text) {
        return """
                {"id": "int_123", "steps": [{"type": "model_output", "content": [{"type": "text", "text": "%s"}]}]}
                """.formatted(text);
    }

    void respond(int status, String body) {
        this.status = status;
        this.responseBody = body;
    }

    String baseUrl() {
        return "http://localhost:" + server.getAddress().getPort() + "/v1beta";
    }

    String lastPath() {
        return lastPath;
    }

    String lastApiKey() {
        return lastApiKey;
    }

    String lastRequestBody() {
        return lastRequestBody;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
