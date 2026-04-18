package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.dto.inventory.*;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.AppointmentNotInProcessException;
import com.sparktech.motorx.exception.InsufficientStockException;
import com.sparktech.motorx.exception.SpareNotFoundException;
import com.sparktech.motorx.mapper.PurchaseTransactionMapper;
import com.sparktech.motorx.mapper.SaleTransactionMapper;
import com.sparktech.motorx.repository.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("InventoryTransactionServiceImpl - Unit Tests")
class InventoryTransactionServiceImplTest {

    @Mock
    private JpaPurchaseTransactionRepository purchaseRepository;
    @Mock
    private JpaSaleTransactionRepository saleRepository;
    @Mock
    private JpaSpareRepository spareRepository;
    @Mock
    private JpaAppointmentRepository appointmentRepository;
    @Mock
    private JpaEmployeeRepository employeeRepository;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private ICurrentUserService currentUserService;
    @Mock
    private ILogService logService;
    @Mock
    private INotificationService notificationService;
    @Mock
    private PurchaseTransactionMapper purchaseMapper;
    @Mock
    private SaleTransactionMapper saleMapper;

    @InjectMocks
    private InventoryTransactionServiceImpl sut;

    @Test
    @DisplayName("registerPurchase como ADMIN incrementa stock y actualiza precio")
    void registerPurchaseAsAdminShouldUpdateStockAndPrice() {
        UserEntity admin = user(1L, Role.ADMIN);
        Spare spare = spare(10L, "Filtro", 5, new BigDecimal("100"), false);
        CreatePurchaseTransactionDTO dto = new CreatePurchaseTransactionDTO(
                "Proveedor",
                List.of(new CreatePurchaseItemDTO(10L, 4, new BigDecimal("120")))
        );

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(spareRepository.findById(10L)).thenReturn(Optional.of(spare));
        when(purchaseRepository.save(any(PurchaseTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseMapper.toResponseDTO(any(PurchaseTransaction.class))).thenReturn(
                new PurchaseTransactionResponseDTO(1L, "Proveedor", LocalDateTime.now(), 1L, "admin@test.com", new BigDecimal("480"), List.of())
        );

        PurchaseTransactionResponseDTO result = sut.registerPurchase(dto);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(spare.getQuantity()).isEqualTo(9);
        assertThat(spare.getPurchasePriceWithVat()).isEqualByComparingTo("120");
        verify(employeeRepository, never()).findByUserId(anyLong());
        verify(logService).logSuccess(eq(LogServiceName.INVENTORY), eq(LogActionType.REGISTER_PURCHASE), eq("admin@test.com"), eq(1L), contains("Compra registrada"));
    }

    @Test
    @DisplayName("registerPurchase como WAREHOUSE_WORKER permite operación")
    void registerPurchaseAsWarehouseShouldBeAllowed() {
        UserEntity employeeUser = user(2L, Role.WAREHOUSE_WORKER);
        Spare spare = spare(11L, "Bujia", 1, new BigDecimal("20"), false);
        CreatePurchaseTransactionDTO dto = new CreatePurchaseTransactionDTO(
                "Proveedor",
                List.of(new CreatePurchaseItemDTO(11L, 1, new BigDecimal("21")))
        );

        when(currentUserService.getAuthenticatedUser()).thenReturn(employeeUser);
        when(spareRepository.findById(11L)).thenReturn(Optional.of(spare));
        when(purchaseRepository.save(any(PurchaseTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(purchaseMapper.toResponseDTO(any(PurchaseTransaction.class))).thenReturn(
                new PurchaseTransactionResponseDTO(2L, "Proveedor", LocalDateTime.now(), 2L, "employee@test.com", new BigDecimal("21"), List.of())
        );

        PurchaseTransactionResponseDTO result = sut.registerPurchase(dto);

        assertThat(result.id()).isEqualTo(2L);
    }

    @Test
    @DisplayName("registerPurchase lanza SpareNotFoundException si item referencia repuesto inexistente")
    void registerPurchaseShouldFailWhenSpareMissing() {
        UserEntity admin = user(1L, Role.ADMIN);
        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(spareRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.registerPurchase(new CreatePurchaseTransactionDTO("Proveedor", List.of(new CreatePurchaseItemDTO(999L, 1, BigDecimal.ONE)))))
                .isInstanceOf(SpareNotFoundException.class);
        verify(logService).logFailure(eq(LogServiceName.INVENTORY), eq(LogActionType.REGISTER_PURCHASE), eq("admin@test.com"), eq(1L), contains("repuesto"));
    }

    @Test
    @DisplayName("getPurchases valida permisos y mapea resultados")
    void getPurchasesShouldValidateAndMap() {
        PurchaseTransaction tx = new PurchaseTransaction();
        tx.setId(88L);

        when(purchaseRepository.findAllByOrderByTransactionDateDesc()).thenReturn(List.of(tx));
        when(purchaseMapper.toResponseDTO(tx)).thenReturn(
                new PurchaseTransactionResponseDTO(88L, "Proveedor", LocalDateTime.now(), 2L, "employee@test.com", BigDecimal.TEN, List.of())
        );

        List<PurchaseTransactionResponseDTO> result = sut.getPurchases();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().id()).isEqualTo(88L);
    }

    @Test
    @DisplayName("getPurchaseById retorna IllegalArgumentException cuando no existe")
    void getPurchaseByIdShouldThrowWhenMissing() {
        when(purchaseRepository.findById(44L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getPurchaseById(44L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("registerSale con cita en progreso descuenta stock y calcula margen 35%")
    void registerSaleWithAppointmentShouldWork() {
        UserEntity admin = user(1L, Role.ADMIN);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(50L);
        appointment.setStatus(AppointmentStatus.IN_PROGRESS);
        Spare spare = spare(20L, "Pastillas", 10, new BigDecimal("100"), false);

        CreateSaleTransactionDTO dto = new CreateSaleTransactionDTO(50L, List.of(new CreateSaleItemDTO(20L, 3)));

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));
        when(spareRepository.findById(20L)).thenReturn(Optional.of(spare));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleMapper.toResponseDTO(any(SaleTransaction.class))).thenReturn(
                new SaleTransactionResponseDTO(1L, LocalDateTime.now(), 50L, 1L, "admin@test.com", new BigDecimal("405"), List.of())
        );

        SaleTransactionResponseDTO result = sut.registerSale(dto);

        assertThat(result.appointmentId()).isEqualTo(50L);
        assertThat(spare.getQuantity()).isEqualTo(7);

        ArgumentCaptor<SaleTransaction> txCaptor = ArgumentCaptor.forClass(SaleTransaction.class);
        verify(saleRepository).save(txCaptor.capture());
        assertThat(txCaptor.getValue().getItems()).hasSize(1);
        assertThat(txCaptor.getValue().getItems().getFirst().getSalePriceAtMoment()).isEqualByComparingTo("135.00");
        verify(logService).logSuccess(eq(LogServiceName.INVENTORY), eq(LogActionType.REGISTER_SALE), eq("admin@test.com"), eq(1L), contains("Venta registrada"));
    }

    @Test
    @DisplayName("registerSale notifica al admin cuando repuesto queda bajo umbral")
    void registerSaleShouldNotifyAdminsWhenStockIsBelowThreshold() {
        UserEntity admin = user(1L, Role.ADMIN);
        Spare spare = spare(40L, "Filtro", 5, new BigDecimal("30"), false);
        spare.setStockThreshold(4);
        spare.setWarehouseLocation("01-02-03-04");

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(spareRepository.findById(40L)).thenReturn(Optional.of(spare));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleMapper.toResponseDTO(any(SaleTransaction.class))).thenReturn(
                new SaleTransactionResponseDTO(9L, LocalDateTime.now(), null, 1L, "admin@test.com", new BigDecimal("40"), List.of())
        );
        when(userRepository.findByRole(Role.ADMIN)).thenReturn(List.of(admin));

        sut.registerSale(new CreateSaleTransactionDTO(null, List.of(new CreateSaleItemDTO(40L, 2))));

        verify(notificationService).createNotification(argThat((CreateNotificationDTO dto) ->
                dto.userId().equals(1L)
                        && dto.urgency() == NotificationUrgency.CRITICAL
                        && dto.description().contains("umbral")
        ));
    }

    @Test
    @DisplayName("registerSale permite appointmentId null")
    void registerSaleWithoutAppointmentShouldWork() {
        UserEntity admin = user(1L, Role.ADMIN);
        Spare spare = spare(21L, "Aceite", 6, new BigDecimal("80"), true);
        CreateSaleTransactionDTO dto = new CreateSaleTransactionDTO(null, List.of(new CreateSaleItemDTO(21L, 2)));

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(spareRepository.findById(21L)).thenReturn(Optional.of(spare));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleMapper.toResponseDTO(any(SaleTransaction.class))).thenReturn(
                new SaleTransactionResponseDTO(2L, LocalDateTime.now(), null, 1L, "admin@test.com", new BigDecimal("200"), List.of())
        );

        SaleTransactionResponseDTO result = sut.registerSale(dto);

        assertThat(result.appointmentId()).isNull();
        verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("registerSale lanza error cuando cita no está en IN_PROGRESS")
    void registerSaleShouldFailWhenAppointmentNotInProgress() {
        UserEntity admin = user(1L, Role.ADMIN);
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(50L);
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(appointmentRepository.findById(50L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.registerSale(new CreateSaleTransactionDTO(50L, List.of(new CreateSaleItemDTO(21L, 1)))))
                .isInstanceOf(AppointmentNotInProcessException.class);
    }

    @Test
    @DisplayName("registerSale lanza InsufficientStockException cuando no hay stock")
    void registerSaleShouldFailWhenInsufficientStock() {
        UserEntity admin = user(1L, Role.ADMIN);
        Spare spare = spare(30L, "Filtro", 0, new BigDecimal("50"), false);

        when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
        when(spareRepository.findById(30L)).thenReturn(Optional.of(spare));

        assertThatThrownBy(() -> sut.registerSale(new CreateSaleTransactionDTO(null, List.of(new CreateSaleItemDTO(30L, 1)))))
                .isInstanceOf(InsufficientStockException.class);
        verify(logService).logFailure(eq(LogServiceName.INVENTORY), eq(LogActionType.REGISTER_SALE), eq("admin@test.com"), eq(1L), contains("Stock insuficiente"));
    }

    @Test
    @DisplayName("registerSale permite operación para rol RECEPTIONIST")
    void registerSaleShouldAllowReceptionistRole() {
        UserEntity employeeUser = user(2L, Role.RECEPTIONIST);
        Spare spare = spare(30L, "Filtro", 5, new BigDecimal("50"), false);

        when(currentUserService.getAuthenticatedUser()).thenReturn(employeeUser);
        when(spareRepository.findById(30L)).thenReturn(Optional.of(spare));
        when(saleRepository.save(any(SaleTransaction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(saleMapper.toResponseDTO(any(SaleTransaction.class))).thenReturn(
                new SaleTransactionResponseDTO(11L, LocalDateTime.now(), null, 2L, "employee@test.com", BigDecimal.ONE, List.of())
        );

        SaleTransactionResponseDTO result = sut.registerSale(new CreateSaleTransactionDTO(null, List.of(new CreateSaleItemDTO(30L, 1))));

        assertThat(result.id()).isEqualTo(11L);
    }

    @Test
    @DisplayName("getSales permite warehouse_worker y receptionist")
    void getSalesShouldAllowWarehouseAndReceptionist() {
        SaleTransaction tx = new SaleTransaction();
        tx.setId(90L);

        when(saleRepository.findAllByOrderByTransactionDateDesc()).thenReturn(List.of(tx));
        when(saleMapper.toResponseDTO(tx)).thenReturn(
                new SaleTransactionResponseDTO(90L, LocalDateTime.now(), null, 2L, "employee@test.com", BigDecimal.ONE, List.of())
        );

        List<SaleTransactionResponseDTO> result = sut.getSales();

        assertThat(result).hasSize(1);
    }

    @Test
    @DisplayName("getSaleById lanza IllegalArgumentException si no existe")
    void getSaleByIdShouldThrowWhenMissing() {
        when(saleRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getSaleById(3L)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("getTodaySalesSummary suma montos y cuenta transacciones")
    void getTodaySalesSummaryShouldAggregateSales() {
        SaleTransaction tx1 = new SaleTransaction();
        SaleTransaction tx2 = new SaleTransaction();

        when(saleRepository.findByDateRange(any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(tx1, tx2));
        when(saleMapper.toResponseDTO(tx1)).thenReturn(new SaleTransactionResponseDTO(1L, LocalDateTime.now(), null, 1L, "a@a.com", new BigDecimal("100"), List.of()));
        when(saleMapper.toResponseDTO(tx2)).thenReturn(new SaleTransactionResponseDTO(2L, LocalDateTime.now(), null, 1L, "a@a.com", new BigDecimal("40"), List.of()));

        DailySalesSummaryDTO result = sut.getTodaySalesSummary();

        assertThat(result.date()).isEqualTo(LocalDate.now());
        assertThat(result.totalSales()).isEqualByComparingTo("140");
        assertThat(result.transactionCount()).isEqualTo(2);
    }

    private UserEntity user(Long id, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(role.name().toLowerCase() + "@test.com");
        user.setRole(role);
        return user;
    }


    private Spare spare(Long id, String name, Integer qty, BigDecimal purchasePrice, Boolean isOil) {
        Spare spare = new Spare();
        spare.setId(id);
        spare.setName(name);
        spare.setQuantity(qty);
        spare.setStockThreshold(0);
        spare.setWarehouseLocation("01-01-01-01");
        spare.setPurchasePriceWithVat(purchasePrice);
        spare.setIsOil(isOil);
        return spare;
    }
}

