package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IEmployeeService;
import com.sparktech.motorx.dto.employee.CreateEmployeeRequestDTO;
import com.sparktech.motorx.dto.employee.EmployeeResponseDTO;
import com.sparktech.motorx.dto.employee.UpdateEmployeeRequestDTO;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/employees")
@RequiredArgsConstructor
@Tag(name = "Admin - Empleados", description = "CRUD completo de empleados incluyendo creación de usuario")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminEmployeeController {

    private final IEmployeeService employeeService;

    @PostMapping
    @Operation(
            summary = "Crear empleado",
            description = "Crea un empleado y su usuario en la aplicación con rol EMPLOYEE. " +
                    "El empleado queda habilitado y disponible (estado AVAILABLE) desde el inicio."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Empleado creado exitosamente"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "409", description = "El email o DNI ya están registrados",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull EmployeeResponseDTO> createEmployee(
            @Valid @RequestBody CreateEmployeeRequestDTO request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @GetMapping
    @Operation(summary = "Listar todos los empleados", description = "Devuelve la lista completa de empleados con sus datos de usuario.")
    public ResponseEntity<@NotNull List<EmployeeResponseDTO>> getAllEmployees() {
        return ResponseEntity.ok(employeeService.getAllEmployees());
    }

    @GetMapping("/{employeeId}")
    @Operation(summary = "Detalle de un empleado", description = "Obtiene la información completa de un empleado.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado encontrado"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull EmployeeResponseDTO> getEmployeeById(@PathVariable Long employeeId) {
        return ResponseEntity.ok(employeeService.getEmployeeById(employeeId));
    }

    @PutMapping("/{employeeId}")
    @Operation(
            summary = "Actualizar empleado",
            description = "Actualiza el cargo y el estado del empleado (AVAILABLE / NOT_AVAILABLE)."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Empleado actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos inválidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull EmployeeResponseDTO> updateEmployee(
            @PathVariable Long employeeId,
            @Valid @RequestBody UpdateEmployeeRequestDTO request
    ) {
        return ResponseEntity.ok(employeeService.updateEmployee(employeeId, request));
    }

    @DeleteMapping("/{employeeId}")
    @Operation(
            summary = "Eliminar empleado",
            description = "Elimina el empleado y su usuario asociado del sistema. " +
                    "Esta operación es irreversible."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "Empleado eliminado"),
            @ApiResponse(responseCode = "404", description = "Empleado no encontrado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<@NotNull Object> deleteEmployee(@PathVariable Long employeeId) {
        employeeService.deleteEmployee(employeeId);
        return ResponseEntity.noContent().build();
    }
}

