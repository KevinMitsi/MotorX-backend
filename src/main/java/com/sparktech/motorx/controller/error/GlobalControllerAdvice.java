package com.sparktech.motorx.controller.error;

import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.exception.*;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Manejador global de excepciones para todos los controladores REST de la aplicación.
 * Captura tanto las excepciones de dominio propias como las de infraestructura de Spring/Jakarta.
 * Todas las respuestas de error siguen el esquema {@link ResponseErrorDTO}.
 */
@RestControllerAdvice
public class GlobalControllerAdvice {

    private static final String KEY_DETAIL = "detalle";

    // ---------------------------------------------------------------
    // EXCEPCIONES DE AUTENTICACIÓN / TOKENS
    // ----------------------
    //
    // -----------------------------------------

    @ExceptionHandler(BlockedAccountException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleBlockedAccountException(BlockedAccountException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Cuenta bloqueada o inhabilitada",
                Map.of(KEY_DETAIL, ex.getMessage(), "tipo", "BlockedAccountException")
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }
    @ExceptionHandler(InvalidPasswordException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleInvalidPasswordException(InvalidPasswordException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Credenciales inválidas",
                Map.of(KEY_DETAIL, ex.getMessage(), "tipo", "InvalidPasswordException")
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleInvalidTokenException(InvalidTokenException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.UNAUTHORIZED.value(),
                "Token inválido o expirado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
    }

    @ExceptionHandler(RecoveryTokenException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleRecoveryTokenException(RecoveryTokenException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error con el token de recuperación",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(VerificationCodeException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleVerificationCodeException(VerificationCodeException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error en el código de verificación 2FA",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ---------------------------------------------------------------
    // EXCEPCIONES DE ENTIDADES NO ENCONTRADAS
    // ---------------------------------------------------------------

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleUserNotFoundException(UserNotFoundException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Usuario no encontrado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(UserAlreadyDeletedException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleUserAlreadyDeletedException(UserAlreadyDeletedException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El usuario ya ha sido eliminado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(UserAlreadyBlockedException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleUserAlreadyBlockedException(UserAlreadyBlockedException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El usuario ya se encuentra bloqueado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleEmployeeNotFoundException(EmployeeNotFoundException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Empleado no encontrado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(VehicleNotFoundException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleVehicleNotFoundException(VehicleNotFoundException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Vehículo no encontrado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    @ExceptionHandler(AppointmentNotFoundException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleAppointmentNotFoundException(AppointmentNotFoundException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.NOT_FOUND.value(),
                "Cita no encontrada",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error);
    }

    // ---------------------------------------------------------------
    // EXCEPCIONES DE VEHÍCULOS
    // ---------------------------------------------------------------

    @ExceptionHandler(VehicleAlreadyOwnedException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleVehicleAlreadyOwnedException(VehicleAlreadyOwnedException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El vehículo ya pertenece a otro usuario",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    // ---------------------------------------------------------------
    // EXCEPCIONES DE CITAS (AppointmentException y subclases)
    // ---------------------------------------------------------------


    @ExceptionHandler(LicensePlateRestrictionException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleLicensePlateRestrictionException(LicensePlateRestrictionException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El vehículo tiene restricción de movilidad (pico y placa) en la fecha indicada",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(NoAvailableTechnicianException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleNoAvailableTechnicianException(NoAvailableTechnicianException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "No hay técnicos disponibles para el horario solicitado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(TechnicianSlotOccupiedException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleTechnicianSlotOccupiedException(TechnicianSlotOccupiedException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El técnico ya tiene una cita en ese horario",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(VehicleHasActiveAppointmentException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleVehicleHasActiveAppointmentException(VehicleHasActiveAppointmentException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "El vehículo ya tiene una cita activa",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(InvalidAppointmentSlotException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleInvalidAppointmentSlotException(InvalidAppointmentSlotException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "El slot de cita es inválido",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AppointmentOutsideBusinessHoursException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleAppointmentOutsideBusinessHoursException(AppointmentOutsideBusinessHoursException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "La cita está fuera del horario de atención",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(AppointmentTypeNotAllowedForBrandException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleAppointmentTypeNotAllowedForBrandException(AppointmentTypeNotAllowedForBrandException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "El tipo de cita no está permitido para la marca del vehículo",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(ReworkNotBookableOnlineException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleReworkNotBookableOnlineException(ReworkNotBookableOnlineException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                422,
                "Los reprocesos no se pueden agendar en línea",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(422).body(error);
    }

    @ExceptionHandler(AppointmentForbiddenException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleAppointmentForbiddenException(AppointmentException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.FORBIDDEN.value(),
                "No se puedes agendar una cita de una moto que no es suya",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

    /**
     * Captura genérica para AppointmentException y cualquier subclase no mapeada arriba.
     */
    @ExceptionHandler(AppointmentException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleAppointmentException(AppointmentException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error en la gestión de citas",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    // ---------------------------------------------------------------
    // EXCEPCIONES DE ESTADO / ARGUMENTO ILEGAL
    // ---------------------------------------------------------------

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleIllegalArgumentException(IllegalArgumentException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Argumento inválido",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(error);
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleIllegalStateException(IllegalStateException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "Estado de operación inválido",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }


    // ---------------------------------------------------------------
    // VALIDACIONES DE JAKARTA BEAN VALIDATION
    // ---------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        String objectName = ex.getBindingResult().getObjectName();

        Map<String, String> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Valor inválido",
                        (existing, replacement) -> existing + "; " + replacement
                ));

        Map<String, Object> details = new HashMap<>();
        details.put("dtoName", objectName);
        details.put("camposConError", fieldErrors);
        details.put("totalErrores", fieldErrors.size());

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación en " + objectName,
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleConstraintViolationException(ConstraintViolationException ex) {
        Map<String, String> violations = ex.getConstraintViolations().stream()
                .collect(Collectors.toMap(
                        v -> v.getPropertyPath().toString(),
                        ConstraintViolation::getMessage,
                        (existing, replacement) -> existing + "; " + replacement
                ));

        Map<String, Object> details = new HashMap<>();
        details.put("violaciones", violations);
        details.put("totalViolaciones", violations.size());

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error de validación de parámetros",
                details
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ---------------------------------------------------------------
    // ERRORES DE PARSEO / TIPO DE PARÁMETRO
    // ---------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        String msg = ex.getMessage();
        String detailedMessage = "Error al leer el cuerpo de la solicitud";

        if (msg != null) {
            if (msg.contains("JSON parse error")) {
                detailedMessage = "El formato del JSON es inválido";
            } else if (msg.contains("Required request body is missing")) {
                detailedMessage = "El cuerpo de la solicitud es requerido";
            }
        }

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "Error de lectura del cuerpo de la solicitud",
                Map.of("tipo", "HttpMessageNotReadableException", KEY_DETAIL, detailedMessage)
        );
        return ResponseEntity.badRequest().body(error);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        String paramName = ex.getName();
        String requiredType = Optional.ofNullable(ex.getRequiredType())
                .map(Class::getSimpleName)
                .orElse("desconocido");
        String invalidValue = Objects.toString(ex.getValue(), "null");

        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.BAD_REQUEST.value(),
                "El parámetro '" + paramName + "' tiene un formato inválido",
                Map.of(
                        "param", paramName,
                        "valorRecibido", invalidValue,
                        "tipoEsperado", requiredType
                )
        );
        return ResponseEntity.badRequest().body(error);
    }

    // ---------------------------------------------------------------
    // MANEJADOR GENÉRICO (último recurso)
    // ---------------------------------------------------------------

    @ExceptionHandler(Exception.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleGenericException(Exception ex) throws Exception {
        // Relanzar excepciones internas de Spring para no interferir con SpringDoc
        // ni con el manejo propio del framework
        if (ex instanceof org.springframework.security.core.AuthenticationException
                || ex instanceof org.springframework.security.access.AccessDeniedException) {
            throw ex;
        }
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Error interno del servidor",
                Map.of(
                        "tipo", ex.getClass().getSimpleName(),
                        KEY_DETAIL, ex.getMessage() != null ? ex.getMessage() : "Error desconocido"
                )
        );
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleMissingParams(MissingServletRequestParameterException ex) {
        // Tu lógica para construir el cuerpo del error
        ex.getMessage();
        return ResponseEntity.badRequest().body(new ResponseErrorDTO(400, "Parámetro faltante",
                Map.of(
                        "tipo", ex.getClass().getSimpleName(),
                        KEY_DETAIL, ex.getMessage()
                )));
    }


    @ExceptionHandler(ChasisAlreadyRegisteredException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleChasisAlreadyRegisteredException(ChasisAlreadyRegisteredException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.CONFLICT.value(),
                "Número de chasis ya registrado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    @ExceptionHandler(VehicleDoesntBelongToUserException.class)
    public ResponseEntity<@NotNull ResponseErrorDTO> handleVehicleDoesntBelongToUserException(VehicleDoesntBelongToUserException ex) {
        ResponseErrorDTO error = new ResponseErrorDTO(
                HttpStatus.FORBIDDEN.value(),
                "El vehículo no pertenece al usuario autenticado",
                Map.of(KEY_DETAIL, ex.getMessage())
        );
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error);
    }

}
