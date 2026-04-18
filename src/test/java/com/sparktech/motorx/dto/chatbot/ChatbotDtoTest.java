package com.sparktech.motorx.dto.chatbot;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Chatbot DTOs - Unit Tests")
class ChatbotDtoTest {

    private static ValidatorFactory validatorFactory;
    private static Validator validator;

    @BeforeAll
    static void initValidator() {
        validatorFactory = Validation.buildDefaultValidatorFactory();
        validator = validatorFactory.getValidator();
    }

    @AfterAll
    static void closeValidator() {
        if (validatorFactory != null) {
            validatorFactory.close();
        }
    }

    @Test
    @DisplayName("ChatbotRequestDTO acepta mensaje valido")
    void shouldValidateValidChatbotRequest() {
        ChatbotRequestDTO dto = new ChatbotRequestDTO("Hola");

        Set<ConstraintViolation<ChatbotRequestDTO>> violations = validator.validate(dto);

        assertTrue(violations.isEmpty());
        assertEquals("Hola", dto.message());
    }

    @Test
    @DisplayName("ChatbotRequestDTO rechaza message null")
    void shouldRejectNullMessage() {
        ChatbotRequestDTO dto = new ChatbotRequestDTO(null);

        Set<ConstraintViolation<ChatbotRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        ConstraintViolation<ChatbotRequestDTO> violation = violations.iterator().next();
        assertEquals("Message cannot be blank", violation.getMessage());
    }

    @Test
    @DisplayName("ChatbotRequestDTO rechaza message en blanco")
    void shouldRejectBlankMessage() {
        ChatbotRequestDTO dto = new ChatbotRequestDTO("   ");

        Set<ConstraintViolation<ChatbotRequestDTO>> violations = validator.validate(dto);

        assertEquals(1, violations.size());
        ConstraintViolation<ChatbotRequestDTO> violation = violations.iterator().next();
        assertEquals("Message cannot be blank", violation.getMessage());
    }

    @Test
    @DisplayName("Records de request y response cubren equals hashCode toString y accessors")
    void shouldCoverRecordGeneratedMethods() {
        ChatbotRequestDTO requestA = new ChatbotRequestDTO("Hola");
        ChatbotRequestDTO requestB = new ChatbotRequestDTO("Hola");
        ChatbotRequestDTO requestC = new ChatbotRequestDTO("Adios");

        assertEquals(requestA, requestB);
        assertNotEquals(requestA, requestC);
        assertEquals(requestA.hashCode(), requestB.hashCode());
        assertTrue(requestA.toString().contains("message=Hola"));

        ChatbotResponseDTO responseA = new ChatbotResponseDTO("Respuesta");
        ChatbotResponseDTO responseB = new ChatbotResponseDTO("Respuesta");
        ChatbotResponseDTO responseC = new ChatbotResponseDTO("Otra");

        assertEquals(responseA, responseB);
        assertNotEquals(responseA, responseC);
        assertEquals(responseA.hashCode(), responseB.hashCode());
        assertEquals("Respuesta", responseA.reply());
        assertTrue(responseA.toString().contains("reply=Respuesta"));
    }
}

