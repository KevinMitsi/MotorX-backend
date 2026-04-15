package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IInventoryTransactionService;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryTransactionServiceImpl implements IInventoryTransactionService {
    private static final String EMPLOYEE_PROFILE_MISSING = "El usuario no tiene perfil de empleado.";

    private final JpaPurchaseTransactionRepository purchaseRepository;
    private final JpaSaleTransactionRepository saleRepository;
    private final JpaSpareRepository spareRepository;
    private final JpaAppointmentRepository appointmentRepository;
    private final JpaEmployeeRepository employeeRepository;
    private final JpaUserRepository userRepository;
    private final ICurrentUserService currentUserService;
    private final ILogService logService;
    private final INotificationService notificationService;
    private final PurchaseTransactionMapper purchaseMapper;
    private final SaleTransactionMapper saleMapper;

    @Override
    @Transactional
    public PurchaseTransactionResponseDTO registerPurchase(CreatePurchaseTransactionDTO dto) {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        try {
            validateWarehouseOrAdmin(currentUser);

            PurchaseTransaction transaction = PurchaseTransaction.builder()
                    .supplier(dto.supplier())
                    .createdBy(currentUser)
                    .items(new ArrayList<>())
                    .build();

            for (CreatePurchaseItemDTO itemDto : dto.items()) {
                Spare spare = spareRepository.findById(itemDto.spareId())
                        .orElseThrow(() -> new SpareNotFoundException(itemDto.spareId()));

                spare.setQuantity(spare.getQuantity() + itemDto.quantity());
                spare.setPurchasePriceWithVat(itemDto.purchasePriceWithVat());

                PurchaseTransactionItem item = PurchaseTransactionItem.builder()
                        .purchaseTransaction(transaction)
                        .spare(spare)
                        .quantity(itemDto.quantity())
                        .purchasePriceWithVat(itemDto.purchasePriceWithVat())
                        .build();

                transaction.getItems().add(item);
            }

            PurchaseTransaction saved = purchaseRepository.save(transaction);
            logService.logSuccess(
                    LogServiceName.INVENTORY,
                    LogActionType.REGISTER_PURCHASE,
                    currentUser.getEmail(),
                    currentUser.getId(),
                    "Compra registrada: " + saved.getId()
            );
            return purchaseMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.INVENTORY,
                    LogActionType.REGISTER_PURCHASE,
                    currentUser.getEmail(),
                    currentUser.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<PurchaseTransactionResponseDTO> getPurchases() {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        validateWarehouseOrAdmin(currentUser);
        return purchaseRepository.findAllByOrderByTransactionDateDesc().stream().map(purchaseMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public PurchaseTransactionResponseDTO getPurchaseById(Long id) {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        validateWarehouseOrAdmin(currentUser);
        PurchaseTransaction tx = purchaseRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la compra con ID: " + id));
        return purchaseMapper.toResponseDTO(tx);
    }

    @Override
    @Transactional
    public SaleTransactionResponseDTO registerSale(CreateSaleTransactionDTO dto) {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        try {
            validateReceptionistOrAdmin(currentUser);

            AppointmentEntity appointment = null;
            if (dto.appointmentId() != null) {
                appointment = appointmentRepository.findById(dto.appointmentId())
                        .orElseThrow(() -> new IllegalArgumentException("No se encontro la cita con ID: " + dto.appointmentId()));
                if (appointment.getStatus() != AppointmentStatus.IN_PROGRESS) {
                    throw new AppointmentNotInProcessException(dto.appointmentId());
                }
            }

            SaleTransaction transaction = SaleTransaction.builder()
                    .appointment(appointment)
                    .createdBy(currentUser)
                    .items(new ArrayList<>())
                    .build();

            for (CreateSaleItemDTO itemDto : dto.items()) {
                Spare spare = spareRepository.findById(itemDto.spareId())
                        .orElseThrow(() -> new SpareNotFoundException(itemDto.spareId()));

                int finalQty = spare.getQuantity() - itemDto.quantity();
                if (finalQty < 0) {
                    throw new InsufficientStockException("Stock insuficiente para el repuesto: " + spare.getName());
                }
                spare.setQuantity(finalQty);
                notifyAdminsWhenBelowThreshold(spare);

                BigDecimal margin = Boolean.TRUE.equals(spare.getIsOil()) ? BigDecimal.valueOf(0.25) : BigDecimal.valueOf(0.35);
                BigDecimal salePrice = spare.getPurchasePriceWithVat().multiply(BigDecimal.ONE.add(margin));

                SaleTransactionItem item = SaleTransactionItem.builder()
                        .saleTransaction(transaction)
                        .spare(spare)
                        .quantity(itemDto.quantity())
                        .salePriceAtMoment(salePrice)
                        .build();
                transaction.getItems().add(item);
            }

            SaleTransaction saved = saleRepository.save(transaction);
            logService.logSuccess(
                    LogServiceName.INVENTORY,
                    LogActionType.REGISTER_SALE,
                    currentUser.getEmail(),
                    currentUser.getId(),
                    "Venta registrada: " + saved.getId()
            );
            return saleMapper.toResponseDTO(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.INVENTORY,
                    LogActionType.REGISTER_SALE,
                    currentUser.getEmail(),
                    currentUser.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SaleTransactionResponseDTO> getSales() {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        validateSalesViewer(currentUser);
        return saleRepository.findAllByOrderByTransactionDateDesc().stream().map(saleMapper::toResponseDTO).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SaleTransactionResponseDTO getSaleById(Long id) {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        validateReceptionistOrAdmin(currentUser);
        SaleTransaction tx = saleRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("No se encontro la venta con ID: " + id));
        return saleMapper.toResponseDTO(tx);
    }

    @Override
    @Transactional(readOnly = true)
    public DailySalesSummaryDTO getTodaySalesSummary() {
        UserEntity currentUser = currentUserService.getAuthenticatedUser();
        validateReceptionistOrAdmin(currentUser);

        LocalDate today = LocalDate.now();
        LocalDateTime start = LocalDateTime.of(today, LocalTime.MIN);
        LocalDateTime end = LocalDateTime.of(today.plusDays(1), LocalTime.MIN);

        List<SaleTransactionResponseDTO> sales = saleRepository.findByDateRange(start, end)
                .stream().map(saleMapper::toResponseDTO).toList();

        BigDecimal total = sales.stream()
                .map(SaleTransactionResponseDTO::totalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new DailySalesSummaryDTO(today, total, sales.size(), sales);
    }

    private void validateWarehouseOrAdmin(UserEntity user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        EmployeeEntity employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(EMPLOYEE_PROFILE_MISSING));
        if (employee.getPosition() != EmployeePosition.WAREHOUSE_WORKER) {
            throw new IllegalStateException("No tienes permisos para operaciones de bodega.");
        }
    }

    private void validateReceptionistOrAdmin(UserEntity user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        EmployeeEntity employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(EMPLOYEE_PROFILE_MISSING));
        if (employee.getPosition() != EmployeePosition.RECEPCIONISTA) {
            throw new IllegalStateException("Solo recepcion o admin puede registrar ventas.");
        }
    }

    private void validateSalesViewer(UserEntity user) {
        if (user.getRole() == Role.ADMIN) {
            return;
        }
        EmployeeEntity employee = employeeRepository.findByUserId(user.getId())
                .orElseThrow(() -> new IllegalStateException(EMPLOYEE_PROFILE_MISSING));
        if (employee.getPosition() != EmployeePosition.RECEPCIONISTA
                && employee.getPosition() != EmployeePosition.WAREHOUSE_WORKER) {
            throw new IllegalStateException("No tienes permisos para ver ventas.");
        }
    }

    private void notifyAdminsWhenBelowThreshold(Spare spare) {
        if (spare.getStockThreshold() == null || spare.getStockThreshold() <= 0) {
            return;
        }
        if (spare.getQuantity() >= spare.getStockThreshold()) {
            return;
        }

        List<UserEntity> admins = userRepository.findByRole(Role.ADMIN);
        for (UserEntity admin : admins) {
            notificationService.createNotification(new CreateNotificationDTO(
                    admin.getId(),
                    "Stock crítico de repuesto",
                    "El repuesto " + spare.getName() + " quedó en stock " + spare.getQuantity() +
                            ", por debajo del umbral " + spare.getStockThreshold() +
                            ". Ubicación: " + spare.getWarehouseLocation(),
                    NotificationUrgency.CRITICAL,
                    "INVENTORY"
            ));
        }
    }
}

