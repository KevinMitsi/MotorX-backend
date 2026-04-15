package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.inventory.*;

import java.util.List;

public interface ISpareService {
    SpareResponseDTO createSpare(CreateSpareDTO dto);

    List<SpareResponseDTO> getAllSpares();

    List<SpareResponseDTO> getSparesBelowThreshold();

    SpareResponseDTO getSpareById(Long id);

    SpareResponseDTO updateSpare(Long id, UpdateSpareDTO dto);

    SpareResponseDTO updatePurchasePrice(Long id, UpdateSparePurchasePriceDTO dto);

    void deleteSpare(Long id);

    long notifyWarehouseWorkersToRestock(Long spareId);
}

