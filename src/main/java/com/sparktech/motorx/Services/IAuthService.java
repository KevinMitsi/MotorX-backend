package com.sparktech.motorx.Services;

import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.exception.UserNotFoundException;

public interface IAuthService {
    String login(LoginRequestDTO loginRequest) throws InvalidPasswordException;
    AuthResponseDTO register(RegisterUserDTO registerRequest);
    UserDTO getCurrentUser() throws UserNotFoundException;
    void logout();
    AuthResponseDTO refreshToken(String refreshToken) throws InvalidPasswordException;
    AuthResponseDTO verify2FA(String email, String code) throws InvalidPasswordException;
}
