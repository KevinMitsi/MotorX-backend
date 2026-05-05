package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IProcedureService;
import com.sparktech.motorx.dto.procedure.CreateProcedureDTO;
import com.sparktech.motorx.dto.procedure.ProcedureResponseDTO;
import com.sparktech.motorx.dto.procedure.UpdateProcedureDTO;
import com.sparktech.motorx.dto.procedure.UpdateServiceProceduresDTO;
import com.sparktech.motorx.entity.ProcedureEntity;
import com.sparktech.motorx.entity.ServiceEntity;
import com.sparktech.motorx.exception.DuplicateProcedureNameException;
import com.sparktech.motorx.exception.ProcedureNotFoundException;
import com.sparktech.motorx.exception.ServiceNotFoundException;
import com.sparktech.motorx.mapper.ProcedureMapper;
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
public class ProcedureServiceImpl implements IProcedureService {

    private final JpaProcedureRepository procedureRepository;
    private final JpaServiceRepository serviceRepository;
    private final ProcedureMapper procedureMapper;

    @Override
    @Transactional
    public ProcedureResponseDTO create(CreateProcedureDTO dto) {
        if (procedureRepository.existsByName(dto.name())) {
            throw new DuplicateProcedureNameException(dto.name());
        }
        ProcedureEntity entity = procedureMapper.toEntity(dto);
        ProcedureEntity saved = procedureRepository.save(entity);
        return procedureMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional
    public ProcedureResponseDTO update(Long id, UpdateProcedureDTO dto) {
        ProcedureEntity entity = findProcedureOrThrow(id);
        if (procedureRepository.existsByNameAndIdNot(dto.name(), id)) {
            throw new DuplicateProcedureNameException(dto.name());
        }
        procedureMapper.updateEntity(entity, dto);
        ProcedureEntity saved = procedureRepository.save(entity);
        return procedureMapper.toResponseDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public ProcedureResponseDTO getById(Long id) {
        return procedureMapper.toResponseDTO(findProcedureOrThrow(id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> getAll() {
        return procedureRepository.findAll().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> getActive() {
        return procedureRepository.findByActiveTrueOrderByNameAsc().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcedureResponseDTO> getProceduresByService(Long serviceId) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));
        return service.getBaseProcedures().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional
    public List<ProcedureResponseDTO> updateServiceProcedures(Long serviceId, UpdateServiceProceduresDTO dto) {
        ServiceEntity service = serviceRepository.findById(serviceId)
                .orElseThrow(() -> new ServiceNotFoundException(serviceId));

        Set<ProcedureEntity> newProcedures = new HashSet<>();
        for (Long procedureId : dto.procedureIds()) {
            newProcedures.add(findProcedureOrThrow(procedureId));
        }

        service.setBaseProcedures(newProcedures);
        ServiceEntity saved = serviceRepository.save(service);
        return saved.getBaseProcedures().stream()
                .map(procedureMapper::toResponseDTO)
                .toList();
    }

    private ProcedureEntity findProcedureOrThrow(Long id) {
        return procedureRepository.findById(id)
                .orElseThrow(() -> new ProcedureNotFoundException(id));
    }
}

