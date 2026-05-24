package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.IEmailNotificationService;
import com.sparktech.motorx.Services.IInventoryTransactionService;
import com.sparktech.motorx.Services.IOrderService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.inventory.CreateSaleItemDTO;
import com.sparktech.motorx.dto.inventory.CreateSaleTransactionDTO;
import com.sparktech.motorx.dto.order.AddProcedureToOrderDTO;
import com.sparktech.motorx.dto.order.AddSpareToOrderDTO;
import com.sparktech.motorx.dto.order.OrderResponseDTO;
import com.sparktech.motorx.dto.order.UpdateOrderProcedureCostDTO;
import com.sparktech.motorx.dto.order.TechnicianDailyOrderDTO;
import com.sparktech.motorx.dto.appointment.TechnicianAppointmentSummaryDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.*;
import com.sparktech.motorx.mapper.OrderServiceMapper;
import com.sparktech.motorx.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements IOrderService {

    private final JpaOrderServiceRepository orderRepository;
    private final JpaAppointmentRepository appointmentRepository;
    private final JpaEmployeeRepository employeeRepository;
    private final JpaProcedureRepository procedureRepository;
    private final JpaSpareRepository spareRepository;
    private final IInventoryTransactionService inventoryTransactionService;
    private final IEmailNotificationService emailNotificationService;
    private final ICurrentUserService currentUserService;
    private final ILogService logService;
    private final OrderServiceMapper orderMapper;

    @Override
    @Transactional
    public OrderResponseDTO createOrder(Long appointmentId) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

            if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
                throw new AppointmentNotInProcessException(appointmentId);
            }

            EmployeeEntity employee = resolveTechnicianOrThrow(actor, appointment);

            Optional<OrderServiceEntity> existing = orderRepository.findByAppointmentId(appointmentId);
            if (existing.isPresent()) {
                return orderMapper.toResponseDTO(existing.get());
            }

            OrderServiceEntity order = new OrderServiceEntity();
            order.setAppointment(appointment);
            order.setEmployee(employee);
            order.setStartDate(LocalDateTime.now());
            order.setEndDate(null);
            order.setStatus(OrderStatus.IN_PROGRESS);
            order.setTotalServices(BigDecimal.ZERO);
            order.setTotalSpareParts(BigDecimal.ZERO);
            order.setTotalToPay(BigDecimal.ZERO);

            OrderServiceEntity saved = orderRepository.save(order);
            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.CREATE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    "Orden creada para cita " + appointmentId
            );
            return orderMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.CREATE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDTO getOrderByAppointment(Long appointmentId) {
        OrderServiceEntity order = orderRepository.findDetailedByAppointmentId(appointmentId)
                .orElseThrow(() -> new OrderServiceNotFoundException(appointmentId));
        return orderMapper.toResponseDTO(order);
    }

    @Override
    @Transactional
    public OrderResponseDTO addProcedure(Long orderId, AddProcedureToOrderDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            OrderServiceEntity order = getEditableOrder(orderId);
            ProcedureEntity procedure = procedureRepository.findById(dto.procedureId())
                    .orElseThrow(() -> new ProcedureNotFoundException(dto.procedureId()));

            boolean exists = order.getProcedures().stream()
                    .anyMatch(p -> p.getProcedure().getId().equals(dto.procedureId()));
            if (exists) {
                throw new IllegalArgumentException("El procedimiento ya esta agregado a la orden");
            }

            OrderProcedureEntity orderProcedure = new OrderProcedureEntity();
            orderProcedure.setId(new OrderProcedureId(order.getId(), procedure.getId()));
            orderProcedure.setOrder(order);
            orderProcedure.setProcedure(procedure);
            orderProcedure.setCost(dto.cost());

            order.getProcedures().add(orderProcedure);
            recalculateTotals(order);

            OrderServiceEntity saved = orderRepository.save(order);
            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.ADD_ORDER_PROCEDURE,
                    actor.getEmail(),
                    actor.getId(),
                    "Procedimiento agregado a orden " + orderId
            );
            return orderMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.ADD_ORDER_PROCEDURE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO updateProcedureCost(Long orderId, Long procedureId, UpdateOrderProcedureCostDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            OrderServiceEntity order = getEditableOrder(orderId);
            OrderProcedureEntity orderProcedure = order.getProcedures().stream()
                    .filter(p -> p.getProcedure().getId().equals(procedureId))
                    .findFirst()
                    .orElseThrow(() -> new ProcedureNotFoundException(procedureId));

            orderProcedure.setCost(dto.cost());
            recalculateTotals(order);

            OrderServiceEntity saved = orderRepository.save(order);
            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.UPDATE_ORDER_PROCEDURE,
                    actor.getEmail(),
                    actor.getId(),
                    "Costo actualizado en orden " + orderId
            );
            return orderMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.UPDATE_ORDER_PROCEDURE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO addSpare(Long orderId, AddSpareToOrderDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            OrderServiceEntity order = getEditableOrder(orderId);
            Spare spare = spareRepository.findById(dto.spareId())
                    .orElseThrow(() -> new SpareNotFoundException(dto.spareId()));

            BigDecimal unitPrice = calculateSalePrice(spare);
            inventoryTransactionService.registerSale(new CreateSaleTransactionDTO(
                    order.getAppointment().getId(),
                    List.of(new CreateSaleItemDTO(dto.spareId(), dto.quantity()))
            ));

            OrderSpareEntity orderSpare = order.getSpares().stream()
                    .filter(s -> s.getSpare().getId().equals(dto.spareId()))
                    .findFirst()
                    .orElse(null);

            if (orderSpare == null) {
                orderSpare = new OrderSpareEntity();
                orderSpare.setId(new OrderSpareId(order.getId(), spare.getId()));
                orderSpare.setOrder(order);
                orderSpare.setSpare(spare);
                orderSpare.setQuantity(dto.quantity());
                orderSpare.setUnitPrice(unitPrice);
                order.getSpares().add(orderSpare);
            } else {
                orderSpare.setQuantity(orderSpare.getQuantity() + dto.quantity());
            }

            recalculateTotals(order);

            OrderServiceEntity saved = orderRepository.save(order);
            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.ADD_ORDER_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    "Repuesto agregado a orden " + orderId
            );
            return orderMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.ADD_ORDER_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public OrderResponseDTO completeOrder(Long orderId) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            OrderServiceEntity order = getEditableOrder(orderId);
            order.setEndDate(LocalDateTime.now());
            order.setStatus(OrderStatus.COMPLETED);

            OrderServiceEntity saved = orderRepository.save(order);
            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.COMPLETE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    "Orden completada " + orderId
            );
            return orderMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.COMPLETE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public void sendServiceDetails(Long orderId) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            OrderServiceEntity order = orderRepository.findDetailedById(orderId)
                    .orElseThrow(() -> new OrderServiceNotFoundException(orderId));

            VehicleEntity vehicle = order.getAppointment().getVehicle();
            UserEntity owner = vehicle.getOwner();
            if (owner == null || owner.getEmail() == null || owner.getEmail().isBlank()) {
                throw new IllegalStateException("La motocicleta no tiene un usuario asociado con correo válido");
            }

            String subject = "Detalle de servicio - orden #" + order.getId();
            emailNotificationService.sendTemplatedMail(
                    owner.getEmail(),
                    subject,
                    "service-details.html",
                    buildServiceDetailsPlaceholders(order)
            );

            logService.logSuccess(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.COMPLETE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    "Detalle de servicio enviado para orden " + orderId
            );
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SERVICE_ORDER,
                    LogActionType.COMPLETE_SERVICE_ORDER,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<TechnicianDailyOrderDTO> getMyTodayOrders() {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        EmployeeEntity employee = employeeRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new EmployeeNotFoundException(actor.getId()));

        LocalDate today = LocalDate.now();
        LocalDateTime start = today.atStartOfDay();
        LocalDateTime end = today.plusDays(1).atStartOfDay();

        List<AppointmentEntity> appointments = appointmentRepository
                .findByTechnicianIdAndStatusAndProcessStartedAtBetween(
                        employee.getId(),
                        AppointmentStatus.IN_PROGRESS,
                        start,
                        end
                );

        if (appointments.isEmpty()) {
            return List.of();
        }

        List<Long> appointmentIds = appointments.stream()
                .map(AppointmentEntity::getId)
                .toList();

        Map<Long, Long> orderIdsByAppointment = orderRepository.findByAppointmentIdIn(appointmentIds)
                .stream()
                .collect(Collectors.toMap(
                        order -> order.getAppointment().getId(),
                        OrderServiceEntity::getId,
                        (existing, replacement) -> existing
                ));

        return appointments.stream()
                .map(appointment -> {
                    VehicleEntity vehicle = appointment.getVehicle();
                    return new TechnicianDailyOrderDTO(
                            appointment.getId(),
                            orderIdsByAppointment.get(appointment.getId()),
                            vehicle.getLicensePlate(),
                            vehicle.getBrand(),
                            vehicle.getModel(),
                            appointment.getAppointmentDate(),
                            appointment.getStartTime(),
                            appointment.getProcessStartedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public TechnicianAppointmentSummaryDTO getAppointmentSummary(Long appointmentId) {
        AppointmentEntity appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        UserEntity actor = currentUserService.getAuthenticatedUser();
        resolveTechnicianOrThrow(actor, appointment);

        VehicleEntity vehicle = appointment.getVehicle();
        EmployeeEntity technician = appointment.getTechnician();
        UserEntity client = vehicle.getOwner();

        return new TechnicianAppointmentSummaryDTO(
                appointment.getId(),
                appointment.getAppointmentType(),
                appointment.getStatus(),
                appointment.getAppointmentDate(),
                appointment.getStartTime(),
                appointment.getEndTime(),
                vehicle.getId(),
                vehicle.getLicensePlate(),
                vehicle.getBrand(),
                vehicle.getModel(),
                appointment.getCurrentMileage(),
                appointment.getClientNotes(),
                client != null ? client.getName() : null,
                technician != null ? technician.getId() : null,
                technician != null ? technician.getUser().getName() : null
        );
    }
    public List<TechnicianDailyOrderDTO> getMyActiveOrders() {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        EmployeeEntity employee = employeeRepository.findByUserId(actor.getId())
                .orElseThrow(() -> new EmployeeNotFoundException(actor.getId()));

        List<AppointmentEntity> appointments = appointmentRepository
                .findByTechnicianIdAndStatusOrderByAppointmentDateDescStartTimeDesc(
                        employee.getId(),
                        AppointmentStatus.IN_PROGRESS
                );

        if (appointments.isEmpty()) {
            return List.of();
        }

        List<Long> appointmentIds = appointments.stream()
                .map(AppointmentEntity::getId)
                .toList();

        Map<Long, Long> orderIdsByAppointment = orderRepository.findByAppointmentIdIn(appointmentIds)
                .stream()
                .collect(Collectors.toMap(
                        order -> order.getAppointment().getId(),
                        OrderServiceEntity::getId,
                        (existing, replacement) -> existing
                ));

        return appointments.stream()
                .map(appointment -> {
                    VehicleEntity vehicle = appointment.getVehicle();
                    return new TechnicianDailyOrderDTO(
                            appointment.getId(),
                            orderIdsByAppointment.get(appointment.getId()),
                            vehicle.getLicensePlate(),
                            vehicle.getBrand(),
                            vehicle.getModel(),
                            appointment.getAppointmentDate(),
                            appointment.getStartTime(),
                            appointment.getProcessStartedAt()
                    );
                })
                .toList();
    }

    private OrderServiceEntity getEditableOrder(Long orderId) {
        OrderServiceEntity order = orderRepository.findDetailedById(orderId)
                .orElseThrow(() -> new OrderServiceNotFoundException(orderId));

        if (order.getStatus() == OrderStatus.COMPLETED || order.getStatus() == OrderStatus.CANCELLED) {
            throw new IllegalStateException("La orden no permite modificaciones");
        }

        UserEntity actor = currentUserService.getAuthenticatedUser();
        resolveTechnicianOrThrow(actor, order.getAppointment());
        return order;
    }

    private EmployeeEntity resolveTechnicianOrThrow(UserEntity user, AppointmentEntity appointment) {
        EmployeeEntity employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new EmployeeNotFoundException(user.getId()));

        if (appointment.getTechnician() == null ||
                !appointment.getTechnician().getId().equals(employee.getId())) {
            throw new TechnicianNotAssignedException(appointment.getId());
        }
        return employee;
    }

    private void recalculateTotals(OrderServiceEntity order) {
        BigDecimal totalServices = order.getProcedures().stream()
                .map(OrderProcedureEntity::getCost)
                .map(this::safeMoney)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSpares = order.getSpares().stream()
                .map(s -> safeMoney(s.getUnitPrice()).multiply(BigDecimal.valueOf(s.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        order.setTotalServices(totalServices.setScale(2, RoundingMode.HALF_UP));
        order.setTotalSpareParts(totalSpares.setScale(2, RoundingMode.HALF_UP));
        order.setTotalToPay(order.getTotalServices().add(order.getTotalSpareParts()).setScale(2, RoundingMode.HALF_UP));
    }

    private BigDecimal calculateSalePrice(Spare spare) {
        BigDecimal margin = Boolean.TRUE.equals(spare.getIsOil())
                ? BigDecimal.valueOf(0.25)
                : BigDecimal.valueOf(0.35);
        return safeMoney(spare.getPurchasePriceWithVat())
                .multiply(BigDecimal.ONE.add(margin))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, String> buildServiceDetailsPlaceholders(OrderServiceEntity order) {
        VehicleEntity vehicle = order.getAppointment().getVehicle();
        UserEntity owner = vehicle.getOwner();

        Map<String, String> placeholders = new LinkedHashMap<>();
        placeholders.put("CLIENT_NAME", owner != null ? safeText(owner.getName()) : "Cliente");
        placeholders.put("CLIENT_EMAIL", owner != null ? safeText(owner.getEmail()) : "");
        placeholders.put("ORDER_ID", String.valueOf(order.getId()));
        placeholders.put("APPOINTMENT_ID", String.valueOf(order.getAppointment().getId()));
        placeholders.put("TECHNICIAN_NAME", resolveTechnicianName(order));
        placeholders.put("VEHICLE_INFO", safeText(vehicle.getBrand()) + " " + safeText(vehicle.getModel()) + " (" + safeText(vehicle.getLicensePlate()) + ")");
        placeholders.put("ORDER_STATUS", order.getStatus().name());
        placeholders.put("ORDER_DATE", order.getStartDate() != null ? order.getStartDate().toLocalDate().toString() : "");
        placeholders.put("PROCEDURE_ROWS", buildProcedureRows(order));
        placeholders.put("SPARE_ROWS", buildSpareRows(order));
        placeholders.put("TOTAL_SERVICES", formatMoney(order.getTotalServices()));
        placeholders.put("TOTAL_SPARE_PARTS", formatMoney(order.getTotalSpareParts()));
        placeholders.put("TOTAL_TO_PAY", formatMoney(order.getTotalToPay()));
        return placeholders;
    }

    private String buildProcedureRows(OrderServiceEntity order) {
        if (order.getProcedures().isEmpty()) {
            return "<tr><td colspan='3' style='padding:12px;text-align:center;color:#6b7280;'>Sin procedimientos registrados</td></tr>";
        }

        return order.getProcedures().stream()
                .map(item -> "<tr>"
                        + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>" + escapeHtml(item.getProcedure().getName()) + "</td>"
                        + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>" + formatMoney(item.getCost()) + "</td>"
                        + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>" + formatMoney(item.getCost()) + "</td>"
                        + "</tr>")
                .collect(Collectors.joining());
    }

    private String buildSpareRows(OrderServiceEntity order) {
        if (order.getSpares().isEmpty()) {
            return "<tr><td colspan='5' style='padding:12px;text-align:center;color:#6b7280;'>Sin repuestos registrados</td></tr>";
        }

        return order.getSpares().stream()
                .map(item -> {
                    BigDecimal lineTotal = safeMoney(item.getUnitPrice()).multiply(BigDecimal.valueOf(item.getQuantity()));
                    return "<tr>"
                            + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;'>" + escapeHtml(item.getSpare().getName()) + "</td>"
                            + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>" + item.getQuantity() + "</td>"
                            + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>" + formatMoney(item.getUnitPrice()) + "</td>"
                            + "<td style='padding:10px;border-bottom:1px solid #e5e7eb;text-align:right;'>" + formatMoney(lineTotal) + "</td>"
                            + "</tr>";
                })
                .collect(Collectors.joining());
    }

    private BigDecimal safeMoney(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatMoney(BigDecimal value) {
        return safeMoney(value).setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private String safeText(String value) {
        return value == null ? "" : escapeHtml(value);
    }

    private String resolveTechnicianName(OrderServiceEntity order) {
        if (order.getEmployee() == null || order.getEmployee().getUser() == null) {
            return "";
        }
        return safeText(order.getEmployee().getUser().getName());
    }

    private String escapeHtml(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
