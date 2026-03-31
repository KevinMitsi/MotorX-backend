package com.sparktech.motorx.dto.log;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(name = "LogPageResponseDTO", description = "Resultado paginado de consulta de logs")
public record LogPageResponseDTO(
        @Schema(description = "Contenido de la pagina")
        List<LogResponseDTO> content,
        @Schema(description = "Pagina actual (base 0)", example = "0")
        int page,
        @Schema(description = "Tamano de la pagina", example = "20")
        int size,
        @Schema(description = "Total de elementos", example = "142")
        long totalElements,
        @Schema(description = "Total de paginas", example = "8")
        int totalPages,
        @Schema(description = "Es la primera pagina", example = "true")
        boolean first,
        @Schema(description = "Es la ultima pagina", example = "false")
        boolean last,
        @Schema(description = "Contenido vacio", example = "false")
        boolean empty
) {
}

