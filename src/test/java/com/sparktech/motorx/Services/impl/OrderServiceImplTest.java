package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IInventoryTransactionService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.order.AddProcedureToOrderDTO;
import com.sparktech.motorx.dto.order.AddSpareToOrderDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.UpdateOrderProcedureCostDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.*;
import com.sparktech.motorx.mapper.OrderServiceMapper;
import com.sparktech.motorx.repository.*;
import org.junit.jupiter.api.BeforeEach;
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
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl - Unit Tests")
class OrderServiceImplTest {

    @Mock
    private JpaOrderServiceRepository orderRepository;
    @Mock
    private JpaAppointmentRepository appointmentRepository;
    @Mock
    private JpaEmployeeRepository employeeRepository;
    @Mock
    private JpaProcedureRepository procedureRepository;
    @Mock
    private JpaSpareRepository spareRepository;
    @Mock
    private IInventoryTransactionService inventoryTransactionService;
    @Mock
    private IEmailNotificationService emailNotificationService;
    @Mock
    private ICurrentUserService currentUserService;
    @Mock
    private ILogService logService;
    @Mock
    private OrderServiceMapper orderMapper;

    @InjectMocks
    private OrderServiceImpl sut;

    private EmployeeEntity technician;

    @BeforeEach
    void setUp() {
        UserEntity actor = new UserEntity();
        actor.setId(10L);
        actor.setEmail("tech@test.com");

        technician = new EmployeeEntity();
        technician.setId(20L);
        technician.setUser(actor);

        lenient().when(currentUserService.getAuthenticatedUser()).thenReturn(actor);
        lenient().when(orderMapper.toResponseDTO(any(OrderServiceEntity.class))).thenReturn(dummyResponse());
        lenient().when(inventoryTransactionService.registerSale(any())).thenReturn(null);
    }

    @Test
    @DisplayName("createOrder crea orden cuando la cita esta en progreso")
    void createOrderShouldCreateOrderWhenAppointmentInProgress() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(orderRepository.findByAppointmentId(5L)).thenReturn(Optional.empty());
        when(orderRepository.save(any(OrderServiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        sut.createOrder(5L);

        ArgumentCaptor<OrderServiceEntity> captor = ArgumentCaptor.forClass(OrderServiceEntity.class);
        verify(orderRepository).save(captor.capture());
        OrderServiceEntity saved = captor.getValue();
        assertThat(saved.getStatus()).isEqualTo(OrderStatus.IN_PROGRESS);
        assertThat(saved.getTotalServices()).isEqualByComparingTo("0");
        assertThat(saved.getTotalSpareParts()).isEqualByComparingTo("0");
        assertThat(saved.getTotalToPay()).isEqualByComparingTo("0");
        assertThat(saved.getStartDate()).isNotNull();
        assertThat(saved.getEndDate()).isNull();
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.CREATE_SERVICE_ORDER), eq("tech@test.com"), eq(10L), contains("Orden creada"));
    }

    @Test
    @DisplayName("createOrder retorna orden existente")
    void createOrderShouldReturnExistingOrder() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity existing = order(OrderStatus.IN_PROGRESS, appointment);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(orderRepository.findByAppointmentId(5L)).thenReturn(Optional.of(existing));

        OrderResponseDTO response = sut.createOrder(5L);

        assertThat(response.id()).isEqualTo(1L);
        verify(orderRepository, never()).save(any(OrderServiceEntity.class));
    }

    @Test
    @DisplayName("createOrder falla si la cita no esta en progreso")
    void createOrderShouldFailWhenAppointmentNotInProgress() {
        AppointmentEntity appointment = appointment(AppointmentStatus.SCHEDULED, technician);
        when(appointmentRepository.findById(5L)).thenReturn(Optional.of(appointment));

        assertThatThrownBy(() -> sut.createOrder(5L)).isInstanceOf(AppointmentNotInProcessException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.CREATE_SERVICE_ORDER), eq("tech@test.com"), eq(10L), contains("cita"));
    }

    @Test
    @DisplayName("getOrderByAppointment lanza OrderServiceNotFoundException")
    void getOrderByAppointmentShouldThrowWhenMissing() {
        when(orderRepository.findDetailedByAppointmentId(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.getOrderByAppointment(7L)).isInstanceOf(OrderServiceNotFoundException.class);
    }

    @Test
    @DisplayName("addProcedure agrega procedimiento y recalcula totales")
    void addProcedureShouldAddAndRecalculateTotals() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        ProcedureEntity procedure = procedure();
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(procedureRepository.findById(30L)).thenReturn(Optional.of(procedure));
        when(orderRepository.save(order)).thenReturn(order);

        sut.addProcedure(1L, new AddProcedureToOrderDTO(30L, new BigDecimal("50")));

        assertThat(order.getProcedures()).hasSize(1);
        assertThat(order.getTotalServices()).isEqualByComparingTo("50");
        assertThat(order.getTotalToPay()).isEqualByComparingTo("50");
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_PROCEDURE), eq("tech@test.com"), eq(10L), contains("Procedimiento agregado"));
    }

    @Test
    @DisplayName("addProcedure falla con procedimiento duplicado")
    void addProcedureShouldFailWhenDuplicate() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        order.getProcedures().add(orderProcedure(order, procedure(), new BigDecimal("40")));
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(procedureRepository.findById(30L)).thenReturn(Optional.of(procedure()));

        assertThatThrownBy(() -> sut.addProcedure(1L, new AddProcedureToOrderDTO(30L, new BigDecimal("50"))))
                .isInstanceOf(IllegalArgumentException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_PROCEDURE), eq("tech@test.com"), eq(10L), contains("procedimiento"));
    }

    @Test
    @DisplayName("updateProcedureCost actualiza costo")
    void updateProcedureCostShouldUpdateCost() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        ProcedureEntity procedure = procedure();
        OrderProcedureEntity orderProcedure = orderProcedure(order, procedure, new BigDecimal("40"));
        order.getProcedures().add(orderProcedure);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(orderRepository.save(order)).thenReturn(order);

        sut.updateProcedureCost(1L, 30L, new UpdateOrderProcedureCostDTO(new BigDecimal("70")));

        assertThat(orderProcedure.getCost()).isEqualByComparingTo("70");
        assertThat(order.getTotalServices()).isEqualByComparingTo("70");
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.UPDATE_ORDER_PROCEDURE), eq("tech@test.com"), eq(10L), contains("Costo actualizado"));
    }

    @Test
    @DisplayName("updateProcedureCost falla si no existe procedimiento en la orden")
    void updateProcedureCostShouldFailWhenProcedureMissing() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));

        assertThatThrownBy(() -> sut.updateProcedureCost(1L, 99L, new UpdateOrderProcedureCostDTO(new BigDecimal("70"))))
                .isInstanceOf(ProcedureNotFoundException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.UPDATE_ORDER_PROCEDURE), eq("tech@test.com"), eq(10L), contains("99"));
    }

    @Test
    @DisplayName("addSpare falla por stock insuficiente")
    void addSpareShouldFailWhenInsufficientStock() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        Spare spare = spare(false, new BigDecimal("100"), 1);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(spareRepository.findById(40L)).thenReturn(Optional.of(spare));
        when(inventoryTransactionService.registerSale(any())).thenThrow(new InsufficientStockException("Stock insuficiente para el repuesto: Filtro"));

        assertThatThrownBy(() -> sut.addSpare(1L, new AddSpareToOrderDTO(40L, 2)))
                .isInstanceOf(InsufficientStockException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_SPARE), eq("tech@test.com"), eq(10L), contains("Stock"));
    }

    @Test
    @DisplayName("addSpare agrega repuesto aceite con margen 25%")
    void addSpareShouldAddOilSpareWithMargin() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        Spare spare = spare(true, new BigDecimal("100"), 10);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(spareRepository.findById(40L)).thenReturn(Optional.of(spare));
        when(orderRepository.save(order)).thenReturn(order);

        sut.addSpare(1L, new AddSpareToOrderDTO(40L, 2));

        assertThat(order.getSpares()).hasSize(1);
        OrderSpareEntity added = order.getSpares().getFirst();
        assertThat(added.getUnitPrice()).isEqualByComparingTo("125.00");
        assertThat(order.getTotalSpareParts()).isEqualByComparingTo("250.00");
        verify(inventoryTransactionService).registerSale(argThat(dto ->
                dto != null
                        && dto.appointmentId().equals(5L)
                        && dto.items().size() == 1
                        && dto.items().getFirst().spareId().equals(40L)
                        && dto.items().getFirst().quantity().equals(2)
        ));
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_SPARE), eq("tech@test.com"), eq(10L), contains("Repuesto agregado"));
    }

    @Test
    @DisplayName("addSpare incrementa cantidad cuando ya existe")
    void addSpareShouldIncreaseQuantityWhenExisting() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        Spare spare = spare(false, new BigDecimal("100"), 10);
        OrderSpareEntity existing = orderSpare(order, spare, 1, new BigDecimal("135"));
        order.getSpares().add(existing);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(spareRepository.findById(40L)).thenReturn(Optional.of(spare));
        when(orderRepository.save(order)).thenReturn(order);

        sut.addSpare(1L, new AddSpareToOrderDTO(40L, 2));

        assertThat(existing.getQuantity()).isEqualTo(3);
        assertThat(order.getTotalSpareParts()).isEqualByComparingTo("405");
        verify(inventoryTransactionService).registerSale(any());
    }

    @Test
    @DisplayName("completeOrder marca orden como completada")
    void completeOrderShouldSetCompleted() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(orderRepository.save(order)).thenReturn(order);

        sut.completeOrder(1L);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        assertThat(order.getEndDate()).isNotNull();
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.COMPLETE_SERVICE_ORDER), eq("tech@test.com"), eq(10L), contains("Orden completada"));
    }

    @Test
    @DisplayName("sendServiceDetails envía HTML al correo del propietario")
    void sendServiceDetailsShouldSendEmailToOwner() {
        UserEntity owner = new UserEntity();
        owner.setId(30L);
        owner.setName("Carlos Cliente");
        owner.setEmail("owner@test.com");

        VehicleEntity vehicle = vehicle("ABC123");
        vehicle.setOwner(owner);

        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        appointment.setVehicle(vehicle);

        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        order.getProcedures().add(orderProcedure(order, procedure(), new BigDecimal("50")));
        order.getSpares().add(orderSpare(order, spare(false, new BigDecimal("100"), 10), 2, new BigDecimal("135")));
        order.setTotalServices(new BigDecimal("50"));
        order.setTotalSpareParts(new BigDecimal("270"));
        order.setTotalToPay(new BigDecimal("320"));

        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));

        sut.sendServiceDetails(1L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<Map<String, String>> placeholdersCaptor = ArgumentCaptor.forClass((Class<Map<String, String>>) (Class<?>) Map.class);
        verify(emailNotificationService).sendTemplatedMail(eq("owner@test.com"), contains("orden #1"), eq("service-details.html"), placeholdersCaptor.capture());
        assertThat(placeholdersCaptor.getValue()).containsEntry("TOTAL_TO_PAY", "320.00");
        assertThat(placeholdersCaptor.getValue().get("PROCEDURE_ROWS")).contains("Cambio aceite");
        assertThat(placeholdersCaptor.getValue().get("SPARE_ROWS")).contains("Filtro");
        verify(logService).logSuccess(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.COMPLETE_SERVICE_ORDER), eq("tech@test.com"), eq(10L), contains("enviado"));
    }

    @Test
    @DisplayName("addSpare falla cuando la orden esta completada")
    void addSpareShouldFailWhenOrderCompleted() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        OrderServiceEntity order = order(OrderStatus.COMPLETED, appointment);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> sut.addSpare(1L, new AddSpareToOrderDTO(40L, 1)))
                .isInstanceOf(IllegalStateException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_SPARE), eq("tech@test.com"), eq(10L), contains("orden"));
    }

    @Test
    @DisplayName("addProcedure falla cuando tecnico no esta asignado")
    void addProcedureShouldFailWhenTechnicianNotAssigned() {
        AppointmentEntity appointment = appointment(AppointmentStatus.IN_PROGRESS, technician);
        EmployeeEntity otherTech = new EmployeeEntity();
        otherTech.setId(99L);
        appointment.setTechnician(otherTech);
        OrderServiceEntity order = order(OrderStatus.IN_PROGRESS, appointment);
        when(orderRepository.findDetailedById(1L)).thenReturn(Optional.of(order));
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));

        assertThatThrownBy(() -> sut.addProcedure(1L, new AddProcedureToOrderDTO(30L, new BigDecimal("50"))))
                .isInstanceOf(TechnicianNotAssignedException.class);

        verify(logService).logFailure(eq(LogServiceName.SERVICE_ORDER), eq(LogActionType.ADD_ORDER_PROCEDURE), eq("tech@test.com"), eq(10L), contains("tecnico"));
    }

    @Test
    @DisplayName("getMyTodayOrders lista citas con recepcion confirmada hoy")
    void getMyTodayOrdersShouldReturnTodayAppointments() {
        AppointmentEntity first = appointment(AppointmentStatus.IN_PROGRESS, technician);
        first.setId(1L);
        first.setAppointmentDate(LocalDate.now());
        first.setStartTime(LocalTime.of(9, 0));
        first.setProcessStartedAt(LocalDateTime.now().minusMinutes(15));
        first.setVehicle(vehicle("ABC123"));

        AppointmentEntity second = appointment(AppointmentStatus.IN_PROGRESS, technician);
        second.setId(2L);
        second.setAppointmentDate(LocalDate.now());
        second.setStartTime(LocalTime.of(10, 0));
        second.setProcessStartedAt(LocalDateTime.now().minusMinutes(5));
        second.setVehicle(vehicle("XYZ987"));

        OrderServiceEntity order = new OrderServiceEntity();
        order.setId(100L);
        order.setAppointment(first);

        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(appointmentRepository.findByTechnicianIdAndStatusAndProcessStartedAtBetween(
                eq(20L),
                eq(AppointmentStatus.IN_PROGRESS),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        )).thenReturn(List.of(first, second));
        when(orderRepository.findByAppointmentIdIn(List.of(1L, 2L))).thenReturn(List.of(order));

        List<com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO> result = sut.getMyTodayOrders();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().appointmentId()).isEqualTo(1L);
        assertThat(result.getFirst().orderId()).isEqualTo(100L);
        assertThat(result.get(1).orderId()).isNull();
    }

    @Test
    @DisplayName("getMyActiveOrders lista todas las citas IN_PROGRESS sin filtro de fecha")
    void getMyActiveOrdersShouldReturnAllInProgressAppointments() {
        AppointmentEntity yesterday = appointment(AppointmentStatus.IN_PROGRESS, technician);
        yesterday.setId(1L);
        yesterday.setAppointmentDate(LocalDate.now().minusDays(1));
        yesterday.setStartTime(LocalTime.of(15, 0));
        yesterday.setProcessStartedAt(LocalDateTime.now().minusDays(1).minusHours(2));
        yesterday.setVehicle(vehicle("ABC123"));

        AppointmentEntity today = appointment(AppointmentStatus.IN_PROGRESS, technician);
        today.setId(2L);
        today.setAppointmentDate(LocalDate.now());
        today.setStartTime(LocalTime.of(9, 0));
        today.setProcessStartedAt(LocalDateTime.now().minusMinutes(30));
        today.setVehicle(vehicle("XYZ987"));

        OrderServiceEntity order = new OrderServiceEntity();
        order.setId(100L);
        order.setAppointment(today);

        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(appointmentRepository.findByTechnicianIdAndStatusOrderByAppointmentDateDescStartTimeDesc(
                20L, AppointmentStatus.IN_PROGRESS
        )).thenReturn(List.of(yesterday, today));
        when(orderRepository.findByAppointmentIdIn(List.of(1L, 2L))).thenReturn(List.of(order));

        List<com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO> result = sut.getMyActiveOrders();

        assertThat(result).hasSize(2);
        assertThat(result.getFirst().appointmentId()).isEqualTo(1L);
        assertThat(result.getFirst().orderId()).isNull();
        assertThat(result.get(1).appointmentId()).isEqualTo(2L);
        assertThat(result.get(1).orderId()).isEqualTo(100L);
        verify(appointmentRepository).findByTechnicianIdAndStatusOrderByAppointmentDateDescStartTimeDesc(20L, AppointmentStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("getMyActiveOrders retorna lista vacia cuando no hay citas activas")
    void getMyActiveOrdersShouldReturnEmptyWhenNoAppointments() {
        when(employeeRepository.findByUserId(10L)).thenReturn(Optional.of(technician));
        when(appointmentRepository.findByTechnicianIdAndStatusOrderByAppointmentDateDescStartTimeDesc(
                20L, AppointmentStatus.IN_PROGRESS
        )).thenReturn(List.of());

        List<com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO> result = sut.getMyActiveOrders();

        assertThat(result).isEmpty();
        verify(orderRepository, never()).findByAppointmentIdIn(any());
    }

    private AppointmentEntity appointment(AppointmentStatus status, EmployeeEntity assignedTechnician) {
        AppointmentEntity appointment = new AppointmentEntity();
        appointment.setId(5L);
        appointment.setStatus(status);
        appointment.setTechnician(assignedTechnician);
        return appointment;
    }

    private OrderServiceEntity order(OrderStatus status, AppointmentEntity appointment) {
        OrderServiceEntity order = new OrderServiceEntity();
        order.setId(1L);
        order.setAppointment(appointment);
        order.setEmployee(technician);
        order.setStartDate(LocalDateTime.now());
        order.setStatus(status);
        order.setTotalServices(BigDecimal.ZERO);
        order.setTotalSpareParts(BigDecimal.ZERO);
        order.setTotalToPay(BigDecimal.ZERO);
        order.setProcedures(new ArrayList<>());
        order.setSpares(new ArrayList<>());
        return order;
    }

    private ProcedureEntity procedure() {
        ProcedureEntity procedure = new ProcedureEntity();
        procedure.setId(30L);
        procedure.setName("Cambio aceite");
        return procedure;
    }

    private OrderProcedureEntity orderProcedure(OrderServiceEntity order, ProcedureEntity procedure, BigDecimal cost) {
        OrderProcedureEntity op = new OrderProcedureEntity();
        op.setOrder(order);
        op.setProcedure(procedure);
        op.setCost(cost);
        op.setId(new OrderProcedureId(order.getId(), procedure.getId()));
        return op;
    }

    private Spare spare(boolean isOil, BigDecimal purchasePrice, int quantity) {
        Spare spare = new Spare();
        spare.setId(40L);
        spare.setName("Filtro");
        spare.setIsOil(isOil);
        spare.setPurchasePriceWithVat(purchasePrice);
        spare.setQuantity(quantity);
        return spare;
    }

    private OrderSpareEntity orderSpare(OrderServiceEntity order, Spare spare, int quantity, BigDecimal unitPrice) {
        OrderSpareEntity os = new OrderSpareEntity();
        os.setOrder(order);
        os.setSpare(spare);
        os.setQuantity(quantity);
        os.setUnitPrice(unitPrice);
        os.setId(new OrderSpareId(order.getId(), spare.getId()));
        return os;
    }

    private VehicleEntity vehicle(String licensePlate) {
        VehicleEntity vehicle = new VehicleEntity();
        vehicle.setLicensePlate(licensePlate);
        vehicle.setBrand("Honda");
        vehicle.setModel("CB190");
        return vehicle;
    }

    private OrderResponseDTO dummyResponse() {
        return new OrderResponseDTO(
                1L,
                5L,
                20L,
                LocalDateTime.now(),
                null,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                OrderStatus.IN_PROGRESS,
                List.of(),
                List.of()
        );
    }
}

