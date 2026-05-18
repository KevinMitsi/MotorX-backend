package com.sparktech.motorx.Services.impl;

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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OurServicesServiceImpl - Unit Tests")
class OurServicesServiceImplTest {

    @Mock
    private JpaServiceRepository serviceRepository;
    @Mock
    private JpaProcedureRepository procedureRepository;
    @Mock
    private ServiceMapper serviceMapper;
    @Mock
    private ProcedureMapper procedureMapper;

    @InjectMocks
    private OurServicesServiceImpl sut;

    @Test
    @DisplayName("create guarda servicio y retorna DTO")
    void createShouldSaveService() {
        CreateServiceDTO dto = new CreateServiceDTO("Mantenimiento", "Desc", 60, new BigDecimal("120"), true, List.of());
        ServiceEntity entity = new ServiceEntity();
        ServiceEntity saved = new ServiceEntity();
        saved.setId(5L);

        when(serviceRepository.existsByName("Mantenimiento")).thenReturn(false);
        when(serviceMapper.toEntity(dto)).thenReturn(entity);
        when(serviceRepository.save(entity)).thenReturn(saved);
        when(serviceMapper.toResponseDTO(saved)).thenReturn(response(5L));

        ServiceResponseDTO result = sut.create(dto);

        assertThat(result.id()).isEqualTo(5L);
    }

    @Test
    @DisplayName("create lanza DuplicateServiceNameException")
    void createShouldFailWhenDuplicateName() {
        CreateServiceDTO dto = new CreateServiceDTO("Mantenimiento", "Desc", 60, new BigDecimal("120"), true, List.of());
        when(serviceRepository.existsByName("Mantenimiento")).thenReturn(true);

        assertThatThrownBy(() -> sut.create(dto)).isInstanceOf(DuplicateServiceNameException.class);
    }

    @Test
    @DisplayName("update actualiza servicio existente")
    void updateShouldUpdateService() {
        UpdateServiceDTO dto = new UpdateServiceDTO("Lavado", "Desc", 30, new BigDecimal("80"), false);
        ServiceEntity entity = service(2L, "Old");
        ServiceEntity saved = service(2L, "Lavado");

        when(serviceRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(serviceRepository.existsByNameAndIdNot("Lavado", 2L)).thenReturn(false);
        when(serviceRepository.save(entity)).thenReturn(saved);
        when(serviceMapper.toResponseDTO(saved)).thenReturn(response(2L));

        ServiceResponseDTO result = sut.update(2L, dto);

        assertThat(result.id()).isEqualTo(2L);
        verify(serviceMapper).updateEntity(entity, dto);
    }

    @Test
    @DisplayName("update lanza DuplicateServiceNameException")
    void updateShouldFailWhenDuplicateName() {
        UpdateServiceDTO dto = new UpdateServiceDTO("Lavado", "Desc", 30, new BigDecimal("80"), false);
        ServiceEntity entity = service(2L, "Old");

        when(serviceRepository.findById(2L)).thenReturn(Optional.of(entity));
        when(serviceRepository.existsByNameAndIdNot("Lavado", 2L)).thenReturn(true);

        assertThatThrownBy(() -> sut.update(2L, dto)).isInstanceOf(DuplicateServiceNameException.class);
    }

    @Test
    @DisplayName("getById lanza ServiceNotFoundException")
    void getByIdShouldThrowWhenMissing() {
        when(serviceRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getById(99L)).isInstanceOf(ServiceNotFoundException.class);
    }

    @Test
    @DisplayName("delete elimina servicio existente")
    void deleteShouldRemoveService() {
        ServiceEntity entity = service(4L, "Lavado");
        when(serviceRepository.findById(4L)).thenReturn(Optional.of(entity));

        sut.delete(4L);

        verify(serviceRepository).delete(entity);
    }

    @Test
    @DisplayName("updateBaseProcedures reemplaza set de procedimientos")
    void updateBaseProceduresShouldReplaceSet() {
        ServiceEntity service = new ServiceEntity();
        service.setId(1L);
        ProcedureEntity p1 = procedure(11L, "P1");
        ProcedureEntity p2 = procedure(12L, "P2");

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(procedureRepository.findById(11L)).thenReturn(Optional.of(p1));
        when(procedureRepository.findById(12L)).thenReturn(Optional.of(p2));
        when(serviceRepository.save(service)).thenReturn(service);
        when(procedureMapper.toResponseDTO(any(ProcedureEntity.class))).thenReturn(responseProcedure(11L), responseProcedure(12L));

        List<ProcedureResponseDTO> result = sut.updateBaseProcedures(1L, new UpdateServiceProceduresDTO(List.of(11L, 12L)));

        assertThat(service.getBaseProcedures()).isEqualTo(Set.of(p1, p2));
        assertThat(result).hasSize(2);
    }

    @Test
    @DisplayName("updateBaseProcedures falla si procedimiento no existe")
    void updateBaseProceduresShouldFailWhenProcedureMissing() {
        ServiceEntity service = new ServiceEntity();
        service.setId(1L);

        when(serviceRepository.findById(1L)).thenReturn(Optional.of(service));
        when(procedureRepository.findById(11L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.updateBaseProcedures(1L, new UpdateServiceProceduresDTO(List.of(11L))))
                .isInstanceOf(ProcedureNotFoundException.class);
    }

    private ServiceEntity service(Long id, String name) {
        ServiceEntity entity = new ServiceEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private ProcedureEntity procedure(Long id, String name) {
        ProcedureEntity entity = new ProcedureEntity();
        entity.setId(id);
        entity.setName(name);
        return entity;
    }

    private ServiceResponseDTO response(Long id) {
        return new ServiceResponseDTO(id, "Servicio", "Desc", 30, new BigDecimal("50"), true, List.of(), null, null);
    }

    private ProcedureResponseDTO responseProcedure(Long id) {
        return new ProcedureResponseDTO(id, "Proc", "Desc", true, null, null);
    }
}

