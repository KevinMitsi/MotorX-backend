package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.procedure.CreateProcedureDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateProcedureDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;

import java.util.List;

public interface IProcedureService {
    ProcedureResponseDTO create(CreateProcedureDTO dto);

    ProcedureResponseDTO update(Long id, UpdateProcedureDTO dto);

    ProcedureResponseDTO getById(Long id);

    List<ProcedureResponseDTO> getAll();

    List<ProcedureResponseDTO> getActive();

    List<ProcedureResponseDTO> getProceduresByService(Long serviceId);

    List<ProcedureResponseDTO> updateServiceProcedures(Long serviceId, UpdateServiceProceduresDTO dto);
}

