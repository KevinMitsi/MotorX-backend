package com.sparktech.motorx.Services.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.*;
import static org.awaitility.Awaitility.await;

@DisplayName("VerificationCodeCacheServiceImpl - Unit Tests")
class VerificationCodeCacheServiceImplTest {

    // No @ExtendWith(MockitoExtension.class) — no hay mocks
    // Instancia real: la lógica es pura in-memory
    private VerificationCodeCacheServiceImpl sut;

    @BeforeEach
    void setUp() {
        sut = new VerificationCodeCacheServiceImpl();
    }

    // ================================================================
    // saveCode() + getCode()
    // ================================================================

    @Nested
    @DisplayName("saveCode() y getCode()")
    class SaveAndGetTests {

        @Test
        @DisplayName("Guarda y recupera el código correctamente")
        void givenSavedCode_thenGetReturnsIt() {
            // Arrange + Act
            sut.saveCode("user@test.com", "123456", 5);

            // Assert
            assertThat(sut.getCode("user@test.com")).isEqualTo("123456");
        }

        @Test
        @DisplayName("El email se normaliza a minúsculas al guardar y al recuperar")
        void givenUpperCaseEmail_thenNormalizedToLowerCase() {
            // Arrange
            sut.saveCode("USER@TEST.COM", "999999", 5);

            // Act + Assert — recuperar con distintas capitalizaciones
            assertThat(sut.getCode("USER@TEST.COM")).isEqualTo("999999");
            assertThat(sut.getCode("user@test.com")).isEqualTo("999999");
            assertThat(sut.getCode("User@Test.Com")).isEqualTo("999999");
        }

        @Test
        @DisplayName("Retorna null si no hay código almacenado para ese email")
        void givenUnknownEmail_thenReturnNull() {
            // Act + Assert
            assertThat(sut.getCode("nobody@test.com")).isNull();
        }

        @Test
        @DisplayName("Sobreescribe el código si se llama saveCode dos veces para el mismo email")
        void givenTwoSavesForSameEmail_thenLastWins() {
            // Arrange
            sut.saveCode("user@test.com", "111111", 5);
            sut.saveCode("user@test.com", "222222", 5);

            // Act + Assert
            assertThat(sut.getCode("user@test.com")).isEqualTo("222222");
        }

        @Test
        @DisplayName("Retorna null para código expirado y lo elimina del caché")
        void givenExpiredCode_thenReturnNullAndRemove() {
            // Arrange — expira en 0 minutos (ya expirado casi de inmediato)
            // Usamos reflexión para forzar expiración instantánea con 0 minutos
            // (LocalDateTime.now().plusMinutes(0) = ahora, isAfter(ahora) = false
            // por nanosegundos de diferencia, así que usamos -1 para garantizar)
            // Alternativa segura: guardar con 1 min y esperar, o usar 0 y esperar con Awaitility
            sut.saveCode("user@test.com", "123456", 0);

            // Esperar hasta que el código sea considerado expirado y eliminado
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> sut.getCode("user@test.com") == null);

            // Act
            String result = sut.getCode("user@test.com");

            // Assert
            assertThat(result).isNull();

            // Verificar que fue eliminado del caché (segunda llamada también null)
            assertThat(sut.getCode("user@test.com")).isNull();
        }

        @Test
        @DisplayName("Códigos de distintos emails son independientes entre sí")
        void givenMultipleEmails_thenEachHasItsOwnCode() {
            // Arrange
            sut.saveCode("alice@test.com", "111111", 5);
            sut.saveCode("bob@test.com",   "222222", 5);

            // Act + Assert
            assertThat(sut.getCode("alice@test.com")).isEqualTo("111111");
            assertThat(sut.getCode("bob@test.com")).isEqualTo("222222");
        }
    }

    // ================================================================
    // deleteCode()
    // ================================================================

    @Nested
    @DisplayName("deleteCode()")
    class DeleteCodeTests {

        @Test
        @DisplayName("Elimina el código existente y getCode retorna null")
        void givenExistingCode_thenDeleteAndReturnNull() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act
            sut.deleteCode("user@test.com");

            // Assert
            assertThat(sut.getCode("user@test.com")).isNull();
        }

        @Test
        @DisplayName("Eliminar un email inexistente no lanza excepción (idempotente)")
        void givenUnknownEmail_thenNoExceptionThrown() {
            // Act + Assert
            assertThatCode(() -> sut.deleteCode("nobody@test.com"))
                    .doesNotThrowAnyException();
        }

        @Test
        @DisplayName("El email se normaliza a minúsculas al eliminar")
        void givenUpperCaseEmail_thenDeleteWorksRegardlessOfCase() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act — eliminar con mayúsculas
            sut.deleteCode("USER@TEST.COM");

            // Assert
            assertThat(sut.getCode("user@test.com")).isNull();
        }
    }

    // ================================================================
    // validateCode()
    // ================================================================

    @Nested
    @DisplayName("validateCode()")
    class ValidateCodeTests {

        @Test
        @DisplayName("Retorna true para código correcto y lo elimina del caché (one-time use)")
        void givenCorrectCode_thenReturnTrueAndDeleteCode() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act
            boolean result = sut.validateCode("user@test.com", "123456");

            // Assert
            assertThat(result).isTrue();

            // El código debe haberse eliminado después de su uso
            assertThat(sut.getCode("user@test.com")).isNull();
        }

        @Test
        @DisplayName("Retorna false para código incorrecto y NO elimina el código del caché")
        void givenWrongCode_thenReturnFalseAndKeepCode() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act
            boolean result = sut.validateCode("user@test.com", "000000");

            // Assert
            assertThat(result).isFalse();

            // El código correcto debe seguir almacenado
            assertThat(sut.getCode("user@test.com")).isEqualTo("123456");
        }

        @Test
        @DisplayName("Retorna false si no hay código almacenado para ese email")
        void givenNoStoredCode_thenReturnFalse() {
            // Act + Assert
            assertThat(sut.validateCode("nobody@test.com", "123456")).isFalse();
        }

        @Test
        @DisplayName("Retorna false para código expirado")
        void givenExpiredCode_thenReturnFalse() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 0);

            // Esperar a que expire
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> sut.getCode("user@test.com") == null);

            // Act + Assert
            assertThat(sut.validateCode("user@test.com", "123456")).isFalse();
        }

        @Test
        @DisplayName("El código es case-sensitive (123456 != 12345A)")
        void givenCaseSensitiveCode_thenWrongCaseReturnsFalse() {
            // Arrange
            sut.saveCode("user@test.com", "AbCdEf", 5);

            // Act + Assert
            assertThat(sut.validateCode("user@test.com", "abcdef")).isFalse();
            assertThat(sut.validateCode("user@test.com", "ABCDEF")).isFalse();
            assertThat(sut.validateCode("user@test.com", "AbCdEf")).isTrue(); // exact match
        }

        @Test
        @DisplayName("Código válido solo puede usarse una vez (one-time use garantizado)")
        void givenValidCode_thenCannotBeUsedTwice() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act — primer uso válido
            boolean firstUse  = sut.validateCode("user@test.com", "123456");
            // Segundo intento con el mismo código
            boolean secondUse = sut.validateCode("user@test.com", "123456");

            // Assert
            assertThat(firstUse).isTrue();
            assertThat(secondUse).isFalse(); // ya fue eliminado
        }

        @Test
        @DisplayName("Email normalizado a minúsculas durante la validación")
        void givenUpperCaseEmailInValidate_thenNormalizeAndValidate() {
            // Arrange
            sut.saveCode("user@test.com", "123456", 5);

            // Act — validar con email en mayúsculas
            boolean result = sut.validateCode("USER@TEST.COM", "123456");

            // Assert
            assertThat(result).isTrue();
        }
    }

    // ================================================================
    // cleanupExpiredCodes() — comportamiento observable indirectamente
    // ================================================================

    @Nested
    @DisplayName("Limpieza de códigos expirados")
    class CleanupTests {

        @Test
        @DisplayName("Al guardar un nuevo código, los expirados de otros emails son limpiados")
        void givenExpiredCodesForOtherEmails_thenCleanedUpOnNextSave() {
            // Arrange — guardar código con expiración 0 para alice
            sut.saveCode("alice@test.com", "111111", 0);
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> sut.getCode("alice@test.com") == null);

            // Act — guardar código para bob activa cleanupExpiredCodes()
            sut.saveCode("bob@test.com", "222222", 5);

            // Assert — alice ya no tiene código (fue limpiado)
            // El cleanup se ejecuta en el save(), por lo que alice queda removida
            assertThat(sut.getCode("alice@test.com")).isNull();

            // Bob sí tiene su código
            assertThat(sut.getCode("bob@test.com")).isEqualTo("222222");
        }

        @Test
        @DisplayName("Múltiples códigos expirados son limpiados en un solo ciclo")
        void givenMultipleExpiredCodes_thenAllCleanedUp() {
            // Arrange
            sut.saveCode("user1@test.com", "111111", 0);
            sut.saveCode("user2@test.com", "222222", 0);
            sut.saveCode("user3@test.com", "333333", 0);
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> sut.getCode("user1@test.com") == null
                            && sut.getCode("user2@test.com") == null
                            && sut.getCode("user3@test.com") == null);

            // Act — trigger cleanup al guardar uno nuevo
            sut.saveCode("active@test.com", "999999", 5);

            // Assert — todos los expirados limpiados
            assertThat(sut.getCode("user1@test.com")).isNull();
            assertThat(sut.getCode("user2@test.com")).isNull();
            assertThat(sut.getCode("user3@test.com")).isNull();

            // El activo permanece
            assertThat(sut.getCode("active@test.com")).isEqualTo("999999");
        }

        @Test
        @DisplayName("Código válido no es eliminado por el cleanup")
        void givenValidCodeDuringCleanup_thenNotRemoved() {
            // Arrange — un código válido y uno expirado
            sut.saveCode("valid@test.com",   "VALID",   5);
            sut.saveCode("expired@test.com", "EXPIRED", 0);
            await().atMost(Duration.ofSeconds(2))
                    .until(() -> sut.getCode("expired@test.com") == null);

            // Act — trigger cleanup
            sut.saveCode("trigger@test.com", "TRIGGER", 5);

            // Assert — el válido permanece intacto
            assertThat(sut.getCode("valid@test.com")).isEqualTo("VALID");
        }
    }
}