package com.sparktech.motorx.mapper;

import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.entity.UserEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface UserEntityMapper {


    @Mapping(source = "id", target = "id")
    @Mapping(source = "name", target = "name")
    @Mapping(source = "dni", target = "dni")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "password", target = "password")
    @Mapping(source = "phone", target = "phone")
    @Mapping(source = "createdAt", target = "createdAt")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "enabled", target = "enabled")
    @Mapping(source = "accountLocked", target = "accountLocked")
    @Mapping(source = "updatedAt", target = "updatedAt")
    UserDTO toUserDTO(UserEntity user);

}
