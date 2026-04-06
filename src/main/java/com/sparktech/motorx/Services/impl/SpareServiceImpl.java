package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ISpareService;
import com.sparktech.motorx.dto.inventory.*;
import com.sparktech.motorx.entity.Spare;
import com.sparktech.motorx.exception.DuplicateSpareCodeException;
import com.sparktech.motorx.exception.InvalidWarehouseLocationException;
import com.sparktech.motorx.exception.SpareNotFoundException;
import com.sparktech.motorx.mapper.SpareMapper;
import com.sparktech.motorx.repository.JpaSpareRepository;
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

    @Override
    @Transactional
    public SpareResponseDTO createSpare(CreateSpareDTO dto) {
        validateCodesForCreate(dto.savCode(), dto.spareCode());
        validateWarehouseLocation(dto.warehouseLocation());

        Spare spare = spareMapper.toEntity(dto);
        Spare saved = spareRepository.save(spare);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<SpareResponseDTO> getAllSpares() {
        return spareRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public SpareResponseDTO getSpareById(Long id) {
        return toResponse(findSpareOrThrow(id));
    }

    @Override
    @Transactional
    public SpareResponseDTO updateSpare(Long id, UpdateSpareDTO dto) {
        Spare spare = findSpareOrThrow(id);
        validateCodesForUpdate(dto.savCode(), dto.spareCode(), id);
        validateWarehouseLocation(dto.warehouseLocation());

        spareMapper.updateEntity(spare, dto);
        return toResponse(spareRepository.save(spare));
    }

    @Override
    @Transactional
    public SpareResponseDTO updatePurchasePrice(Long id, UpdateSparePurchasePriceDTO dto) {
        Spare spare = findSpareOrThrow(id);
        spare.setPurchasePriceWithVat(dto.purchasePriceWithVat());
        return toResponse(spareRepository.save(spare));
    }

    @Override
    @Transactional
    public void deleteSpare(Long id) {
        Spare spare = findSpareOrThrow(id);
        spareRepository.delete(spare);
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

    private SpareResponseDTO toResponse(Spare spare) {
        BigDecimal margin = Boolean.TRUE.equals(spare.getIsOil()) ? BigDecimal.valueOf(0.25) : BigDecimal.valueOf(0.35);
        BigDecimal salePrice = spare.getPurchasePriceWithVat().multiply(BigDecimal.ONE.add(margin));
        return spareMapper.toResponseDTO(spare, salePrice);
    }
}


