package com.sparktech.motorx.Services.impl;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProcedureServiceImpl - Unit Tests")
class ProcedureServiceImplTest {

    @Mock
    private JpaProcedureRepository procedureRepository;
    @Mock
    private JpaServiceRepository serviceRepository;
    @Mock
    private ProcedureMapper procedureMapper;

    @InjectMocks
    private ProcedureServiceImpl sut;

    @Test
    @DisplayName("create guarda procedimiento y retorna DTO")
    void createShouldSaveProcedure() {
        CreateProcedureDTO dto = new CreateProcedureDTO("Lavado", "Desc", true);
        ProcedureEntity entity = new ProcedureEntity();
        ProcedureEntity saved = new ProcedureEntity();
        saved.setId(5L);

        when(procedureRepository.existsByName("Lavado")).thenReturn(false);
        when(procedureMapper.toEntity(dto)).thenReturn(entity);
        when(procedureRepository.save(entity)).thenReturn(saved);
        when(procedureMapper.toResponseDTO(saved)).thenReturn(response(5L));

        ProcedureResponseDTO result = sut.create(dto);

        assertThat(result.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create lanza DuplicateProcedureNameException")
    void createShouldFailWhenDuplicateName() {
        CreateProcedureDTO dto = new CreateProcedureDTO("Lavado", "Desc", true);
        when(procedureRepository.existsByName("Lavado")).thenReturn(true);

        assertThatThrownBy(() -> sut.create(dto)).isInstanceOf(DuplicateProcedureNameException.class);
    }

    @Test
    @DisplayName("update actualiza procedimiento existente")
    void updateShouldUpdateProcedure() {
        UpdateProcedureDTO dto = new UpdateProcedureDTO("Ajuste", "Desc", false);
        ProcedureEntity entity = procedure(2L, "Old");
        ProcedureEntity saved = procedure(2L, "Ajuste");

        when(procedureRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(procedureRepository.existsByNameAndIdNot("Ajuste", 2L)).thenReturn(false);
        when(procedureRepository.save(entity)).thenReturn(saved);
        when(procedureMapper.toResponseDTO(saved)).thenReturn(response(2L));

        ProcedureResponseDTO result = sut.update(2L, dto);

        assertThat(result.id()).isEqualTo(2L);
        verify(procedureMapper).updateEntity(entity, dto);
    }

    @Test
    @DisplayName("update lanza DuplicateProcedureNameException")
    void updateShouldFailWhenDuplicateName() {
        UpdateProcedureDTO dto = new UpdateProcedureDTO("Ajuste", "Desc", false);
        ProcedureEntity entity = procedure(2L, "Old");

        when(procedureRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(procedureRepository.existsByNameAndIdNot("Ajuste", 2L)).thenReturn(true);

        assertThatThrownBy(() -> sut.update(2L, dto)).isInstanceOf(DuplicateProcedureNameException.class);
    }

    @Test
    @DisplayName("getById lanza ProcedureNotFoundException")
    void getByIdShouldThrowWhenMissing() {
        when(procedureRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getById(99L)).isInstanceOf(ProcedureNotFoundException.class);
    }

    @Test
    @DisplayName("getAll retorna lista mapeada")
    void getAllShouldReturnMappedList() {
        when(procedureRepository.findAll()).thenReturn(List.of(procedure(1L, "P1"), procedure(2L, "P2")));
        when(procedureMapper.toResponseDTO(any(ProcedureEntity.class))).thenReturn(response(1L), response(2L));

        List<ProcedureResponseDTO> result = sut.getAll();

        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("getActive retorna lista ordenada")
    void getActiveShouldReturnActiveList() {
        when(procedureRepository.findByActiveTrueOrderByNameAsc()).thenReturn(List.of(procedure(1L, "P1")));
        when(procedureMapper.toResponseDTO(any(ProcedureEntity.class))).thenReturn(response(1L));

        List<ProcedureResponseDTO> result = sut.getActive();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getProceduresByService lanza ServiceNotFoundException")
    void getProceduresByServiceShouldFailWhenServiceMissing() {
        when(serviceRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getProceduresByService(10L)).isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    @DisplayName("updateServiceProcedures reemplaza set de procedimientos")
    void updateServiceProceduresShouldReplaceSet() {
        ServiceEntity service = new ServiceEntity();
        service.setId(1L);
        ProcedureEntity p1 = procedure(11L, "P1");
        ProcedureEntity p2 = procedure(12L, "P2");

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(procedureRepository.findById(11L)).thenReturn(Optional.of(p1));
        when(procedureRepository.findById(12L)).thenReturn(Optional.of(p2));
        when(serviceRepository.save(service)).thenReturn(service);
        when(procedureMapper.toResponseDTO(any(ProcedureEntity.class))).thenReturn(response(11L), response(12L));

        List<ProcedureResponseDTO> result = sut.updateServiceProcedures(1L, new UpdateServiceProceduresDTO(List.of(11L, 12L)));

        assertThat(service.getBaseProcedures()).isEqualTo(Set.of(p1, p2));
        assertThat(result).hasSize(2);
    }

    private ProcedureEntity procedure(Long id, String name) {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private ProcedureResponseDTO response(Long id) {
        return new ProcedureResponseDTO(id, "Procedimiento", "Desc", true, null, null);
    }
}

