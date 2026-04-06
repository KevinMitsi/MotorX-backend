package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.inventory.CreateSpareDTO;
import com.sparktech.motorx.dto.inventory.SpareResponseDTO;
import com.sparktech.motorx.entity.Spare;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;
import org.mapstruct.MappingTarget;

import java.math.BigDecimal;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface SpareMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    Spare toEntity(CreateSpareDTO dto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void updateEntity(@MappingTarget Spare target, com.sparktech.motorx.dto.inventory.UpdateSpareDTO dto);

    @Mapping(target = "salePrice", source = "salePrice")
    SpareResponseDTO toResponseDTO(Spare spare, BigDecimal salePrice);
}

