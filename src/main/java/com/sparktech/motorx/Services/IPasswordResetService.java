package com.sparktech.motorx.Services;


import com.sparktech.motorx.dto.auth.PasswordResetDTO;
import com.sparktech.motorx.dto.auth.PasswordResetRequestDTO;
import com.sparktech.motorx.exception.RecoveryTokenException;
import com.sparktech.motorx.exception.UserNotFoundException;

public interface IPasswordResetService {
    void requestReset(PasswordResetRequestDTO dto) throws UserNotFoundException;
    void resetPassword(PasswordResetDTO dto) throws RecoveryTokenException;
}
