package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.dto.chatbot.ChatbotResponseDTO;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatbotServiceImpl - Unit Tests")
class ChatbotServiceImplTest {

    private static final String GENERIC_ERROR = "Lo siento, no pude procesar tu mensaje. Int\u00e9ntalo de nuevo.";
    private static final String UNAVAILABLE_ERROR = "Lo siento, el servicio de chatbot no est\u00e1 disponible en este momento. Por favor, int\u00e9ntalo m\u00e1s tarde.";

    private ChatbotServiceImpl sut;
    private HttpServer server;

    @BeforeEach
    void setUp() {
        sut = new ChatbotServiceImpl();
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("sendMessage retorna reply del webhook cuando la llamada es exitosa")
    void shouldReturnWebhookReplyWhenExchangeIsSuccessful() throws Exception {
        AtomicReference<String> capturedMethod = new AtomicReference<>();
        AtomicReference<String> capturedContentType = new AtomicReference<>();
        AtomicReference<String> capturedBody = new AtomicReference<>();

        startWebhookServer(exchange -> {
            capturedMethod.set(exchange.getRequestMethod());
            capturedContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            capturedBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

            byte[] responseBody = "{\"reply\":\"Hola desde n8n\"}".getBytes(StandardCharsets.UTF_8);
            sendResponse(exchange, 200, responseBody);
        });

        ChatbotResponseDTO response = sut.sendMessage("Hola");

        assertNotNull(response);
        assertEquals("Hola desde n8n", response.reply());
        assertEquals("POST", capturedMethod.get());
        assertTrue(capturedContentType.get().contains("application/json"));
        assertEquals("{\"message\":\"Hola\"}", capturedBody.get());
    }

    @Test
    @DisplayName("sendMessage retorna mensaje generico cuando webhook responde sin body")
    void shouldReturnGenericMessageWhenWebhookBodyIsNull() throws Exception {
        startWebhookServer(exchange -> sendResponse(exchange, 204, new byte[0]));

        ChatbotResponseDTO response = sut.sendMessage("Hola");

        assertNotNull(response);
        assertEquals(GENERIC_ERROR, response.reply());
    }

    @Test
    @DisplayName("sendMessage retorna mensaje de indisponibilidad cuando ocurre excepcion")
    void shouldReturnUnavailableMessageWhenWebhookCallFails() throws Exception {
        setWebhookUrl("http://localhost:1/webhook-chat");

        ChatbotResponseDTO response = sut.sendMessage("Hola");

        assertNotNull(response);
        assertEquals(UNAVAILABLE_ERROR, response.reply());
    }

    private void startWebhookServer(HttpHandler handler) throws Exception {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/webhook-chat", handler);
        server.start();

        String url = "http://localhost:" + server.getAddress().getPort() + "/webhook-chat";
        setWebhookUrl(url);
    }

    private void sendResponse(HttpExchange exchange, int status, byte[] body) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, body.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    private void setWebhookUrl(String value) throws Exception {
        Field field = ChatbotServiceImpl.class.getDeclaredField("webhookUrl");
        field.setAccessible(true);
        field.set(sut, value);
    }
}
