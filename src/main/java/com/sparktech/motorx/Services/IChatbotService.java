package com.sparktech.motorx.Services;


import com.sparktech.motorx.dto.chatbot.ChatbotResponseDTO;

/**
 * Service for chatbot functionality.
 * Delegates user messages to the n8n webhook and returns the reply.
 */
public interface IChatbotService {

    /**
     * Sends a message to the chatbot webhook and returns the response.
     *
     * @param message the user's message
     * @return chatbot reply
     */
    ChatbotResponseDTO sendMessage(String message);
}

