package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.auth.RegisterUserDTO;

public interface IUserService {

    void register(RegisterUserDTO request);

}
