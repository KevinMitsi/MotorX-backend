package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.Services.INotificationService;
import com.sparktech.motorx.Services.ISpareService;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.inventory.*;
import com.sparktech.motorx.entity.LogActionType;
import com.sparktech.motorx.entity.LogServiceName;
import com.sparktech.motorx.entity.NotificationUrgency;
import com.sparktech.motorx.entity.Spare;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.exception.DuplicateSpareCodeException;
import com.sparktech.motorx.exception.InvalidWarehouseLocationException;
import com.sparktech.motorx.exception.SpareNotFoundException;
import com.sparktech.motorx.mapper.SpareMapper;
import com.sparktech.motorx.repository.JpaSpareRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class SpareServiceImpl implements ISpareService {

    private static final Pattern WAREHOUSE_PATTERN = Pattern.compile("\\d{2}-\\d{2}-\\d{2}-\\d{2}");

    private final JpaSpareRepository spareRepository;
    private final SpareMapper spareMapper;
    private final ICurrentUserService currentUserService;
    private final ILogService logService;
    private final JpaUserRepository userRepository;
    private final INotificationService notificationService;

    @Override
    @Transactional
    public SpareResponseDTO createSpare(CreateSpareDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            validateCodesForCreate(dto.savCode(), dto.spareCode());
            validateWarehouseLocation(dto.warehouseLocation());

            Spare spare = spareMapper.toEntity(dto);
            Spare saved = spareRepository.save(spare);

            logService.logSuccess(
                    LogServiceName.SPARE,
                    LogActionType.CREATE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    "Repuesto creado: " + saved.getId()
            );
            return toResponse(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SPARE,
                    LogActionType.CREATE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpareResponseDTO> getAllSpares() {
        return spareRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpareResponseDTO> searchSpares(String name, String savCode) {
        String normalizedName = normalizeFilter(name);
        String normalizedSavCode = normalizeFilter(savCode);
        return spareRepository.searchByNameAndSavCode(normalizedName, normalizedSavCode)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpareResponseDTO> getSparesBelowThreshold() {
        return spareRepository.findLowStockSpares()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpareResponseDTO getSpareById(Long id) {
        return toResponse(findSpareOrThrow(id));
    }

    @Override
    @Transactional
    public SpareResponseDTO updateSpare(Long id, UpdateSpareDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            Spare spare = findSpareOrThrow(id);
            validateCodesForUpdate(dto.savCode(), dto.spareCode(), id);
            validateWarehouseLocation(dto.warehouseLocation());

            spareMapper.updateEntity(spare, dto);
            Spare saved = spareRepository.save(spare);
            logService.logSuccess(
                    LogServiceName.SPARE,
                    LogActionType.UPDATE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    "Repuesto actualizado: " + saved.getId()
            );
            return toResponse(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SPARE,
                    LogActionType.UPDATE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public SpareResponseDTO updatePurchasePrice(Long id, UpdateSparePurchasePriceDTO dto) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            Spare spare = findSpareOrThrow(id);
            spare.setPurchasePriceWithVat(dto.purchasePriceWithVat());
            Spare saved = spareRepository.save(spare);
            logService.logSuccess(
                    LogServiceName.SPARE,
                    LogActionType.UPDATE_SPARE_PURCHASE_PRICE,
                    actor.getEmail(),
                    actor.getId(),
                    "Precio de compra actualizado para repuesto: " + saved.getId()
            );
            return toResponse(saved);
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SPARE,
                    LogActionType.UPDATE_SPARE_PURCHASE_PRICE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public void deleteSpare(Long id) {
        UserEntity actor = currentUserService.getAuthenticatedUser();
        try {
            Spare spare = findSpareOrThrow(id);
            spareRepository.delete(spare);
            logService.logSuccess(
                    LogServiceName.SPARE,
                    LogActionType.DELETE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    "Repuesto eliminado: " + id
            );
        } catch (RuntimeException ex) {
            logService.logFailure(
                    LogServiceName.SPARE,
                    LogActionType.DELETE_SPARE,
                    actor.getEmail(),
                    actor.getId(),
                    ex.getMessage()
            );
            throw ex;
        }
    }

    @Override
    @Transactional
    public long notifyWarehouseWorkersToRestock(Long spareId) {
        Spare spare = findSpareOrThrow(spareId);

        List<UserEntity> warehouseUsers = userRepository.findByRole(Role.WAREHOUSE_WORKER)
                .stream()
                .filter(UserEntity::isEnabled)
                .toList();

        for (UserEntity warehouseUser : warehouseUsers) {
            notificationService.createNotification(new CreateNotificationDTO(
                    warehouseUser.getId(),
                    "Surtir estanteria de repuesto",
                    "Surtir estanteria " + spare.getWarehouseLocation() + " con el repuesto " + spare.getName() +
                            " (stock actual " + spare.getQuantity() + ", umbral " + spare.getStockThreshold() + ")",
                    NotificationUrgency.HIGH,
                    "SPARE"
            ));
        }
        return warehouseUsers.size();
    }

    private Spare findSpareOrThrow(Long id) {
        return spareRepository.findById(id).orElseThrow(() -> new SpareNotFoundException(id));
    }

    private void validateCodesForCreate(String savCode, String spareCode) {
        if (spareRepository.existsBySavCode(savCode)) {
            throw new DuplicateSpareCodeException("El codigo SAV ya existe: " + savCode);
        }
        if (spareRepository.existsBySpareCode(spareCode)) {
            throw new DuplicateSpareCodeException("El codigo de repuesto ya existe: " + spareCode);
        }
    }

    private void validateCodesForUpdate(String savCode, String spareCode, Long id) {
        if (spareRepository.existsBySavCodeAndIdNot(savCode, id)) {
            throw new DuplicateSpareCodeException("El codigo SAV ya existe: " + savCode);
        }
        if (spareRepository.existsBySpareCodeAndIdNot(spareCode, id)) {
            throw new DuplicateSpareCodeException("El codigo de repuesto ya existe: " + spareCode);
        }
    }

    private void validateWarehouseLocation(String location) {
        if (location == null || !WAREHOUSE_PATTERN.matcher(location).matches()) {
            throw new InvalidWarehouseLocationException(location);
        }
    }

    private String normalizeFilter(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private SpareResponseDTO toResponse(Spare spare) {
        BigDecimal margin = Boolean.TRUE.equals(spare.getIsOil()) ? BigDecimal.valueOf(0.25) : BigDecimal.valueOf(0.35);
        BigDecimal salePrice = spare.getPurchasePriceWithVat().multiply(BigDecimal.ONE.add(margin));
        return spareMapper.toResponseDTO(spare, salePrice);
    }
}


