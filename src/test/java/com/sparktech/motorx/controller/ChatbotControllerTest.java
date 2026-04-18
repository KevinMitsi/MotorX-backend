package com.sparktech.motorx.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sparktech.motorx.Services.IChatbotService;
import com.sparktech.motorx.Services.IMetricsService;
import com.sparktech.motorx.controller.error.GlobalControllerAdvice;
import com.sparktech.motorx.dto.chatbot.ChatbotRequestDTO;
import com.sparktech.motorx.dto.chatbot.ChatbotResponseDTO;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.security.JwtService;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ChatbotController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalControllerAdvice.class, ChatbotControllerTest.TestConfig.class})
@DisplayName("ChatbotController - Tests")
class ChatbotControllerTest {

    @Resource
    private MockMvc mockMvc;

    @Resource
    private IChatbotService chatbotService;

    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        reset(chatbotService);
        objectMapper = new ObjectMapper();
    }

    @Test
    @DisplayName("POST /api/v1/chatbot/message - 200 cuando request es valida")
    void shouldReturn200WhenMessageIsValid() throws Exception {
        ChatbotRequestDTO request = new ChatbotRequestDTO("Hola");
        when(chatbotService.sendMessage("Hola")).thenReturn(new ChatbotResponseDTO("Hola, en que te ayudo?"));

        mockMvc.perform(post("/api/v1/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply", is("Hola, en que te ayudo?")));

        verify(chatbotService).sendMessage("Hola");
    }

    @Test
    @DisplayName("POST /api/v1/chatbot/message - 400 cuando message esta en blanco")
    void shouldReturn400WhenMessageIsBlank() throws Exception {
        ChatbotRequestDTO request = new ChatbotRequestDTO("   ");

        mockMvc.perform(post("/api/v1/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatbotService);
    }

    @Test
    @DisplayName("POST /api/v1/chatbot/message - 400 cuando body no trae message")
    void shouldReturn400WhenMessageIsMissing() throws Exception {
        mockMvc.perform(post("/api/v1/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(chatbotService);
    }

    @Test
    @DisplayName("POST /api/v1/chatbot/message - 400 cuando service lanza IllegalArgumentException")
    void shouldReturn400WhenServiceThrowsIllegalArgumentException() throws Exception {
        ChatbotRequestDTO request = new ChatbotRequestDTO("Hola");
        when(chatbotService.sendMessage(any())).thenThrow(new IllegalArgumentException("Mensaje invalido"));

        mockMvc.perform(post("/api/v1/chatbot/message")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(request)))
                .andExpect(status().isBadRequest());

        verify(chatbotService).sendMessage("Hola");
    }

    private String json(Object o) throws Exception {
        return objectMapper.writeValueAsString(o);
    }

    @TestConfiguration
    static class TestConfig {

        @Bean
        @Primary
        IChatbotService chatbotService() {
            return mock(IChatbotService.class);
        }

        @Bean
        @Primary
        JwtService jwtService() {
            return mock(JwtService.class);
        }

        @Bean
        @Primary
        JwtAuthenticationFilter jwtAuthenticationFilter() {
            return mock(JwtAuthenticationFilter.class);
        }

        @Bean
        @Primary
        CustomUserDetailsService customUserDetailsService() {
            return mock(CustomUserDetailsService.class);
        }

        @Bean
        @Primary
        IMetricsService metricsService() {
            return mock(IMetricsService.class);
        }
    }
}

