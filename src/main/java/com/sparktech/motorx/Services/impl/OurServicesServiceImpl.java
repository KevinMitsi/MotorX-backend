package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IOurServicesService;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;
import com.sparktech.motorx.dto.service.CreateServiceDTO;
import com.sparktech.motorx.dto.service.ServiceResponseDTO;
import com.sparktech.motorx.dto.service.UpdateServiceDTO;
import com.sparktech.motorx.entity.ProcedureEntity;
import com.sparktech.motorx.entity.ServiceEntity;
import com.sparktech.motorx.exception.DuplicateServiceNameException;
import com.sparktech.motorx.exception.ProcedureNotFoundException;
import com.sparktech.motorx.exception.ServiceNotFoundException;
import com.sparktech.motorx.mapper.ProcedureMapper;
import com.sparktech.motorx.mapper.ServiceMapper;
import com.sparktech.motorx.repository.JpaProcedureRepository;
import com.sparktech.motorx.repository.JpaServiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class OurServicesServiceImpl implements IOurServicesService {

    private final JpaServiceRepository serviceRepository;
    private final JpaProcedureRepository procedureRepository;
    private final ServiceMapper serviceMapper;
    private final ProcedureMapper procedureMapper;

    @Override
    @Transactional
    public ServiceResponseDTO create(CreateServiceDTO dto) {
        if (serviceRepository.existsByName(dto.name())) {
            throw new DuplicateServiceNameException(dto.name());
        }

        ServiceEntity entity = serviceMapper.toEntity(dto);
        if (dto.procedureIds() != null && !dto.procedureIds().isEmpty()) {
            entity.setBaseProcedures(resolveProcedures(dto.procedureIds()));
        }

        ServiceEntity saved = serviceRepository.save(entity);
        return serviceMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ServiceResponseDTO update(Long id, UpdateServiceDTO dto) {
        ServiceEntity entity = findServiceOrThrow(id);
        if (serviceRepository.existsByNameAndIdNot(dto.name(), id)) {
            throw new DuplicateServiceNameException(dto.name());
        }
        serviceMapper.updateEntity(entity, dto);
        ServiceEntity saved = serviceRepository.save(entity);
        return serviceMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ServiceResponseDTO getById(Long id) {
        return serviceMapper.toResponseDTO(findServiceOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ServiceResponseDTO> getAll() {
        return serviceRepository.findAll().stream()
                .map(serviceMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ServiceEntity entity = findServiceOrThrow(id);
        serviceRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> getBaseProcedures(Long serviceId) {
        ServiceEntity service = findServiceOrThrow(serviceId);
        return service.getBaseProcedures().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<ProcedureResponseDTO> updateBaseProcedures(Long serviceId, UpdateServiceProceduresDTO dto) {
        ServiceEntity service = findServiceOrThrow(serviceId);
        service.setBaseProcedures(resolveProcedures(dto.procedureIds()));
        ServiceEntity saved = serviceRepository.save(service);
        return saved.getBaseProcedures().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    private ServiceEntity findServiceOrThrow(Long id) {
        return serviceRepository.findById(id)
                .orElseThrow(() -> new ServiceNotFoundException(id));
    }

    private Set<ProcedureEntity> resolveProcedures(List<Long> ids) {
        Set<ProcedureEntity> procedures = new HashSet<>();
        for (Long procedureId : ids) {
            ProcedureEntity procedure = procedureRepository.findById(procedureId)
                    .orElseThrow(() -> new ProcedureNotFoundException(procedureId));
            procedures.add(procedure);
        }
        return procedures;
    }
}

