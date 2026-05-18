package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;
import com.sparktech.motorx.dto.service.CreateServiceDTO;
import com.sparktech.motorx.dto.service.ServiceResponseDTO;
import com.sparktech.motorx.dto.service.UpdateServiceDTO;

import java.util.List;

public interface IOurServicesService {
    ServiceResponseDTO create(CreateServiceDTO dto);

    ServiceResponseDTO update(Long id, UpdateServiceDTO dto);

    ServiceResponseDTO getById(Long id);

    List<ServiceResponseDTO> getAll();

    void delete(Long id);

    List<ProcedureResponseDTO> getBaseProcedures(Long serviceId);

    List<ProcedureResponseDTO> updateBaseProcedures(Long serviceId, UpdateServiceProceduresDTO dto);
}

