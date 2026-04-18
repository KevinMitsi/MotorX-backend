package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IChatbotService;
import com.sparktech.motorx.dto.chatbot.ChatbotResponseDTO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * Implementation of ChatbotService.
 * Forwards messages to the n8n webhook and returns the chatbot reply.
 */
@Service
public class ChatbotServiceImpl implements IChatbotService {

    private static final Logger log = LoggerFactory.getLogger(ChatbotServiceImpl.class);

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${chatbot.webhook-url:http://localhost:5678/webhook/webhook-chat}")
    private String webhookUrl;

    @Override
    public ChatbotResponseDTO sendMessage(String message) {
        log.info("[Chatbot] Sending message to webhook: {}", message);
        log.info("==============================================================================");
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        Map<String, String> body = Map.of("message", message);
        HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<ChatbotResponseDTO> response = restTemplate.exchange(
                    webhookUrl,
                    HttpMethod.POST,
                    request,
                    ChatbotResponseDTO.class
            );

            ChatbotResponseDTO chatbotResponse = response.getBody();
            log.info("[Chatbot] Response received: {}", chatbotResponse);
            return chatbotResponse != null ? chatbotResponse
                    : new ChatbotResponseDTO("Lo siento, no pude procesar tu mensaje. Inténtalo de nuevo.");
        } catch (Exception e) {
            log.error("[Chatbot] Error calling webhook: {}", e.getMessage(), e);
            return new ChatbotResponseDTO("Lo siento, el servicio de chatbot no está disponible en este momento. Por favor, inténtalo más tarde.");
        }
    }
}
