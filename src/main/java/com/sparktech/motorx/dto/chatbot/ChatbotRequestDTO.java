package com.sparktech.motorx.dto.chatbot;

import jakarta.validation.constraints.NotBlank;

public record ChatbotRequestDTO(
        @NotBlank(message = "Message cannot be blank")
        String message
) {}

