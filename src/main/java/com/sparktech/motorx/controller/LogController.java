package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.error.ResponseErrorDTO;
import com.sparktech.motorx.dto.log.LogFilterRequestDTO;
import com.sparktech.motorx.dto.log.LogPageResponseDTO;
import com.sparktech.motorx.dto.log.LogResponseDTO;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogEntity;
import com.sparktech.motorx.entity.LogResult;
import com.sparktech.motorx.entity.LogServiceName;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
@Tag(name = "Admin - Logs", description = "Consulta administrativa de auditoria de eventos")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class LogController {

    private final ILogService logService;

    @GetMapping
    @Operation(
            summary = "Consultar logs de auditoria",
            description = "Retorna logs con paginacion y filtros opcionales por servicio, accion, resultado, actor y rango de fechas."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Consulta exitosa",
                    content = @Content(schema = @Schema(implementation = LogPageResponseDTO.class))),
            @ApiResponse(responseCode = "400", description = "Parametros invalidos",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "401", description = "No autenticado",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "403", description = "Acceso denegado por falta de permisos ADMIN",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class))),
            @ApiResponse(responseCode = "500", description = "Error interno del servidor",
                    content = @Content(schema = @Schema(implementation = ResponseErrorDTO.class)))
    })
    public ResponseEntity<LogPageResponseDTO> getLogs(
            @Parameter(description = "Filtro por modulo", example = "AUTHENTICATION")
            @RequestParam(required = false) LogServiceName serviceName,
            @Parameter(description = "Filtro por accion", example = "LOGIN")
            @RequestParam(required = false) LogActionType actionType,
            @Parameter(description = "Filtro por resultado", example = "SUCCESS")
            @RequestParam(required = false) LogResult result,
            @Parameter(description = "Filtro por correo (coincidencia parcial)", example = "admin@motorx.com")
            @RequestParam(required = false) String actorEmail,
            @Parameter(description = "Filtro por id del actor", example = "5")
            @RequestParam(required = false) Long actorUserId,
            @Parameter(description = "Fecha inicial ISO-8601", example = "2026-03-01T00:00:00")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Fecha final ISO-8601", example = "2026-03-31T23:59:59")
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @ParameterObject
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        LogFilterRequestDTO filter = new LogFilterRequestDTO(
                serviceName,
                actionType,
                result,
                actorEmail,
                actorUserId,
                from,
                to
        );

        Page<LogEntity> page = logService.findLogs(filter, pageable);
        return ResponseEntity.ok(toPageResponse(page));
    }


    private LogPageResponseDTO toPageResponse(Page<LogEntity> page) {
        List<LogResponseDTO> content = page.getContent().stream().map(this::toResponse).toList();
        return new LogPageResponseDTO(
                content,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements(),
                page.getTotalPages(),
                page.isFirst(),
                page.isLast(),
                page.isEmpty()
        );
    }

    private LogResponseDTO toResponse(LogEntity entity) {
        return new LogResponseDTO(
                entity.getId(),
                entity.getServiceName().name(),
                entity.getActionType().name(),
                entity.getResult().name(),
                entity.getActorEmail(),
                entity.getActorUserId(),
                entity.getMessage(),
                entity.getCreatedAt()
        );
    }
}

