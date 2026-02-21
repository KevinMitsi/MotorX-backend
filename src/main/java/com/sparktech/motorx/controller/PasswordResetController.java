package com.sparktech.motorx.controller;

import com.sparktech.motorx.Services.IPasswordResetService;
import com.sparktech.motorx.dto.auth.PasswordResetDTO;
import com.sparktech.motorx.dto.auth.PasswordResetRequestDTO;
import com.sparktech.motorx.exception.RecoveryTokenException;
import com.sparktech.motorx.exception.UserNotFoundException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/password-reset")
@RequiredArgsConstructor
public class PasswordResetController {

    private final IPasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<@NotNull String> requestReset(@RequestBody @Valid PasswordResetRequestDTO dto) throws UserNotFoundException, RecoveryTokenException {
        passwordResetService.requestReset(dto);
        return ResponseEntity.ok("If the email exists, a recovery code has been sent.");
    }

    @PutMapping
    public ResponseEntity<@NotNull String> resetPassword(@RequestBody @Valid PasswordResetDTO dto) throws RecoveryTokenException {
        passwordResetService.resetPassword(dto);
        return ResponseEntity.ok("Password has been successfully reset.");
    }
}