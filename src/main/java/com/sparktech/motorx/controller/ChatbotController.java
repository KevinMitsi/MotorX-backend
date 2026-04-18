package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IChatbotService;
import com.sparktech.motorx.dto.chatbot.ChatbotRequestDTO;
import com.sparktech.motorx.dto.chatbot.ChatbotResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;


/**
 * REST controller for the Quindí Parking chatbot.
 * Forwards messages to the n8n webhook and returns the reply.
 * Accessible by any authenticated user.
 */
@RestController
@RequestMapping("/api/v1/chatbot")
@RequiredArgsConstructor
@Tag(name = "Chatbot", description = "Endpoints for the parking chatbot assistant")
public class ChatbotController {

    private final IChatbotService chatbotService;

    /**
     * Sends a message to the chatbot and returns the reply.
     *
     * @param request the user's message
     * @return chatbot reply
     */
    @Operation(summary = "Send message", description = "Sends a message to the chatbot and returns its reply")
    @ApiResponses( value = {
            @ApiResponse(responseCode = "200", description = "Reply returned successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request (empty message)"),
    })
    @PostMapping("/message")
    public ResponseEntity<ChatbotResponseDTO> sendMessage(@Valid @RequestBody ChatbotRequestDTO request) {
        ChatbotResponseDTO response = chatbotService.sendMessage(request.message());
        return ResponseEntity.ok(response);
    }
}
