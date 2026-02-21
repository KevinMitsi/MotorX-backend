package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UpdateUserRequestDTO;

public interface IUserService {

    void register(RegisterUserDTO request);

    void updateUserDTO(Long userId, UpdateUserRequestDTO userUpdate);

}
