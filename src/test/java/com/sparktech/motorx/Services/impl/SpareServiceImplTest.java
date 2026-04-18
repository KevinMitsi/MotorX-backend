package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.dto.inventory.CreateSpareDTO;
import com.sparktech.motorx.dto.inventory.SpareResponseDTO;
import com.sparktech.motorx.dto.inventory.UpdateSpareDTO;
import com.sparktech.motorx.dto.inventory.UpdateSparePurchasePriceDTO;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.entity.Spare;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.DuplicateSpareCodeException;
import com.sparktech.motorx.exception.InvalidWarehouseLocationException;
import com.sparktech.motorx.exception.SpareNotFoundException;
import com.sparktech.motorx.mapper.SpareMapper;
import com.sparktech.motorx.repository.JpaSpareRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SpareServiceImpl - Unit Tests")
class SpareServiceImplTest {

    @Mock
    private JpaSpareRepository spareRepository;

    @Mock
    private SpareMapper spareMapper;
    @Mock
    private ICurrentUserService currentUserService;
    @Mock
    private ILogService logService;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private INotificationService notificationService;

    @InjectMocks
    private SpareServiceImpl sut;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        UserEntity actor = new UserEntity();
        actor.setId(100L);
        actor.setEmail("admin@test.com");
        lenient().when(currentUserService.getAuthenticatedUser()).thenReturn(actor);
    }

    @Test
    @DisplayName("createSpare guarda y calcula margen 35% para no-aceite")
    void createSpareShouldSaveAndCalculateSalePriceForRegularSpare() {
        CreateSpareDTO dto = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100.00"), false, "Prov", 3, 2, "02-01-01-01");
        Spare entity = spare(false, new BigDecimal("100.00"), 3);
        entity.setId(1L);

        when(spareMapper.toEntity(dto)).thenReturn(entity);
        when(spareRepository.save(entity)).thenReturn(entity);
        when(spareMapper.toResponseDTO(eq(entity), any())).thenAnswer(inv -> {
            BigDecimal salePrice = inv.getArgument(1);
            return response(entity, salePrice);
        });

        SpareResponseDTO result = sut.createSpare(dto);

        assertThat(result.salePrice()).isEqualByComparingTo("135.0000");
        verify(spareRepository).save(entity);
        verify(logService).logSuccess(any(), eq(LogActionType.CREATE_SPARE), anyString(), anyLong(), contains("Repuesto creado"));
    }

    @Test
    @DisplayName("createSpare usa margen 25% para aceites")
    void createSpareShouldUseOilMargin() {
        CreateSpareDTO dto = new CreateSpareDTO("Aceite", "Yamaha", "SAV-2", "REP-2", new BigDecimal("200.00"), true, "Prov", 5, 2, "02-01-01-02");
        Spare entity = spare(true, new BigDecimal("200.00"), 5);
        entity.setId(2L);

        when(spareMapper.toEntity(dto)).thenReturn(entity);
        when(spareRepository.save(entity)).thenReturn(entity);
        when(spareMapper.toResponseDTO(eq(entity), any())).thenAnswer(inv -> response(entity, inv.getArgument(1)));

        SpareResponseDTO result = sut.createSpare(dto);

        assertThat(result.salePrice()).isEqualByComparingTo("250.0000");
    }

    @Test
    @DisplayName("createSpare lanza DuplicateSpareCodeException para SAV duplicado")
    void createSpareShouldFailWhenSavCodeDuplicated() {
        CreateSpareDTO dto = new CreateSpareDTO("Filtro", "AKT", "SAV-DUP", "REP-1", new BigDecimal("100"), false, "Prov", 1, 2, "02-01-01-01");
        when(spareRepository.existsBySavCode("SAV-DUP")).thenReturn(true);

        assertThatThrownBy(() -> sut.createSpare(dto)).isInstanceOf(DuplicateSpareCodeException.class);
        verify(spareRepository, never()).save(any());
        verify(logService).logFailure(any(), eq(LogActionType.CREATE_SPARE), anyString(), anyLong(), contains("SAV"));
    }

    @Test
    @DisplayName("createSpare lanza DuplicateSpareCodeException para spareCode duplicado")
    void createSpareShouldFailWhenSpareCodeDuplicated() {
        CreateSpareDTO dto = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-DUP", new BigDecimal("100"), false, "Prov", 1, 2, "02-01-01-01");
        when(spareRepository.existsBySavCode("SAV-1")).thenReturn(false);
        when(spareRepository.existsBySpareCode("REP-DUP")).thenReturn(true);

        assertThatThrownBy(() -> sut.createSpare(dto)).isInstanceOf(DuplicateSpareCodeException.class);
    }

    @Test
    @DisplayName("createSpare lanza InvalidWarehouseLocationException con formato invalido")
    void createSpareShouldFailWhenWarehouseLocationInvalid() {
        CreateSpareDTO dto = new CreateSpareDTO("Filtro", "AKT", "SAV-1", "REP-1", new BigDecimal("100"), false, "Prov", 1, 2, "2-1-1-1");

        assertThatThrownBy(() -> sut.createSpare(dto)).isInstanceOf(InvalidWarehouseLocationException.class);
        verifyNoInteractions(spareMapper);
    }

    @Test
    @DisplayName("getAllSpares transforma lista")
    void getAllSparesShouldMapAll() {
        Spare s1 = spare(false, new BigDecimal("100"), 1);
        s1.setId(1L);
        Spare s2 = spare(true, new BigDecimal("80"), 2);
        s2.setId(2L);
        when(spareRepository.findAll()).thenReturn(List.of(s1, s2));
        when(spareMapper.toResponseDTO(any(Spare.class), any())).thenAnswer(inv -> response(inv.getArgument(0), inv.getArgument(1)));

        List<SpareResponseDTO> result = sut.getAllSpares();

        assertThat(result).hasSize(2);
        assertThat(result.get(0).salePrice()).isEqualByComparingTo("135.00");
        assertThat(result.get(1).salePrice()).isEqualByComparingTo("100.00");
    }

    @Test
    @DisplayName("getSpareById lanza SpareNotFoundException cuando no existe")
    void getSpareByIdShouldThrowWhenMissing() {
        when(spareRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getSpareById(99L)).isInstanceOf(SpareNotFoundException.class);
    }

    @Test
    @DisplayName("updateSpare actualiza entidad")
    void updateSpareShouldUpdateAndSave() {
        Spare existing = spare(false, new BigDecimal("50"), 3);
        existing.setId(3L);
        UpdateSpareDTO dto = new UpdateSpareDTO("Bujia", "AKT", "SAV-X", "REP-X", new BigDecimal("90"), false, "Prov", 8, 3, "03-01-01-01");

        when(spareRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(spareRepository.save(existing)).thenReturn(existing);
        when(spareMapper.toResponseDTO(eq(existing), any())).thenAnswer(inv -> response(existing, inv.getArgument(1)));

        sut.updateSpare(3L, dto);

        verify(spareMapper).updateEntity(existing, dto);
        verify(spareRepository).save(existing);
        verify(logService).logSuccess(any(), eq(LogActionType.UPDATE_SPARE), anyString(), anyLong(), contains("actualizado"));
    }

    @Test
    @DisplayName("updateSpare valida duplicados para update")
    void updateSpareShouldFailWhenSavCodeAlreadyUsedByOtherRecord() {
        Spare existing = spare(false, new BigDecimal("50"), 3);
        existing.setId(3L);
        UpdateSpareDTO dto = new UpdateSpareDTO("Bujia", "AKT", "SAV-X", "REP-X", new BigDecimal("90"), false, "Prov", 8, 3, "03-01-01-01");

        when(spareRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(spareRepository.existsBySavCodeAndIdNot("SAV-X", 3L)).thenReturn(true);

        assertThatThrownBy(() -> sut.updateSpare(3L, dto)).isInstanceOf(DuplicateSpareCodeException.class);
    }

    @Test
    @DisplayName("updateSpare valida spareCode duplicado para update")
    void updateSpareShouldFailWhenSpareCodeAlreadyUsedByOtherRecord() {
        Spare existing = spare(false, new BigDecimal("50"), 3);
        existing.setId(3L);
        UpdateSpareDTO dto = new UpdateSpareDTO("Bujia", "AKT", "SAV-X", "REP-X", new BigDecimal("90"), false, "Prov", 8, 3, "03-01-01-01");

        when(spareRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(spareRepository.existsBySavCodeAndIdNot("SAV-X", 3L)).thenReturn(false);
        when(spareRepository.existsBySpareCodeAndIdNot("REP-X", 3L)).thenReturn(true);

        assertThatThrownBy(() -> sut.updateSpare(3L, dto)).isInstanceOf(DuplicateSpareCodeException.class);
    }

    @Test
    @DisplayName("updatePurchasePrice cambia solo precio")
    void updatePurchasePriceShouldOnlyChangePrice() {
        Spare existing = spare(false, new BigDecimal("50"), 3);
        existing.setId(3L);
        when(spareRepository.findById(3L)).thenReturn(Optional.of(existing));
        when(spareRepository.save(existing)).thenReturn(existing);
        when(spareMapper.toResponseDTO(eq(existing), any())).thenAnswer(inv -> response(existing, inv.getArgument(1)));

        sut.updatePurchasePrice(3L, new UpdateSparePurchasePriceDTO(new BigDecimal("120")));

        assertThat(existing.getPurchasePriceWithVat()).isEqualByComparingTo("120");
    }

    @Test
    @DisplayName("deleteSpare elimina la entidad")
    void deleteSpareShouldDeleteEntity() {
        Spare existing = spare(false, new BigDecimal("50"), 3);
        existing.setId(5L);
        when(spareRepository.findById(5L)).thenReturn(Optional.of(existing));

        sut.deleteSpare(5L);

        verify(spareRepository).delete(existing);
        verify(logService).logSuccess(any(), eq(LogActionType.DELETE_SPARE), anyString(), anyLong(), contains("eliminado"));
    }

    @Test
    @DisplayName("toResponse envia salePrice al mapper")
    void toResponseShouldPassCalculatedSalePriceToMapper() {
        Spare existing = spare(false, new BigDecimal("100"), 1);
        existing.setId(7L);
        when(spareRepository.findById(7L)).thenReturn(Optional.of(existing));
        when(spareMapper.toResponseDTO(eq(existing), any())).thenReturn(response(existing, new BigDecimal("135")));

        sut.getSpareById(7L);

        ArgumentCaptor<BigDecimal> salePriceCaptor = ArgumentCaptor.forClass(BigDecimal.class);
        verify(spareMapper).toResponseDTO(eq(existing), salePriceCaptor.capture());
        assertThat(salePriceCaptor.getValue()).isEqualByComparingTo("135.00");
    }

    @Test
    @DisplayName("getSparesBelowThreshold retorna solo repuestos bajos")
    void getSparesBelowThresholdShouldReturnMappedValues() {
        Spare s1 = spare(false, new BigDecimal("100"), 1);
        s1.setId(11L);
        when(spareRepository.findLowStockSpares()).thenReturn(List.of(s1));
        when(spareMapper.toResponseDTO(eq(s1), any())).thenAnswer(inv -> response(inv.getArgument(0), inv.getArgument(1)));

        List<SpareResponseDTO> result = sut.getSparesBelowThreshold();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("notifyWarehouseWorkersToRestock notifica a empleados de bodega")
    void notifyWarehouseWorkersToRestockShouldNotifyWarehouseWorkers() {
        Spare spare = spare(false, new BigDecimal("50"), 1);
        spare.setId(70L);
        spare.setStockThreshold(4);
        spare.setWarehouseLocation("03-03-03-03");
        spare.setName("Filtro Premium");

        UserEntity warehouseUser = new UserEntity();
        warehouseUser.setId(501L);
        warehouseUser.setRole(Role.WAREHOUSE_WORKER);
        warehouseUser.setEnabled(true);

        when(spareRepository.findById(70L)).thenReturn(Optional.of(spare));
        when(userRepository.findByRole(Role.WAREHOUSE_WORKER)).thenReturn(List.of(warehouseUser));

        long notified = sut.notifyWarehouseWorkersToRestock(70L);

        assertThat(notified).isEqualTo(1L);
        verify(notificationService).createNotification(argThat((CreateNotificationDTO dto) ->
                dto.userId().equals(501L)
                        && dto.description().contains("03-03-03-03")
                        && dto.description().contains("Filtro Premium")
        ));
    }

    private Spare spare(Boolean isOil, BigDecimal purchasePrice, Integer quantity) {
        Spare spare = new Spare();
        spare.setName("Repuesto");
        spare.setCompatibleMotorcycles("AKT");
        spare.setSavCode("SAV");
        spare.setSpareCode("REP");
        spare.setPurchasePriceWithVat(purchasePrice);
        spare.setIsOil(isOil);
        spare.setSupplier("Proveedor");
        spare.setQuantity(quantity);
        spare.setStockThreshold(2);
        spare.setWarehouseLocation("01-01-01-01");
        return spare;
    }

    private SpareResponseDTO response(Spare spare, BigDecimal salePrice) {
        return new SpareResponseDTO(
                spare.getId(),
                spare.getName(),
                spare.getCompatibleMotorcycles(),
                spare.getSavCode(),
                spare.getSpareCode(),
                spare.getPurchasePriceWithVat(),
                salePrice,
                spare.getIsOil(),
                spare.getSupplier(),
                spare.getQuantity(),
                spare.getStockThreshold(),
                spare.getWarehouseLocation()
        );
    }
}

