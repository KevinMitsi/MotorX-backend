package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IAdminUserService;
import com.sparktech.motorx.dto.user.AdminUserResponseDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.UserAlreadyBlockedException;
import com.sparktech.motorx.exception.UserAlreadyDeletedException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements IAdminUserService {

    private final JpaUserRepository jpaUserRepository;

    // ---------------------------------------------------------------
    // LISTADO Y CONSULTA
    // ---------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public List<AdminUserResponseDTO> getAllUsers() {
        return jpaUserRepository.findAll()
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AdminUserResponseDTO getUserById(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        return toResponseDTO(user);
    }

    // ---------------------------------------------------------------
    // BLOQUEO / DESBLOQUEO
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public AdminUserResponseDTO blockUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);

        if (user.isAccountLocked()) {
            throw new UserAlreadyBlockedException(userId);
        }

        user.setAccountLocked(true);
        jpaUserRepository.save(user);
        return toResponseDTO(user);
    }

    @Override
    @Transactional
    public AdminUserResponseDTO unblockUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);
        user.setAccountLocked(false);
        jpaUserRepository.save(user);
        return toResponseDTO(user);
    }

    // ---------------------------------------------------------------
    // SOFT DELETE
    // ---------------------------------------------------------------

    @Override
    @Transactional
    public void deleteUser(Long userId) {
        UserEntity user = findUserOrThrow(userId);

        if (user.getDeletedAt() != null) {
            throw new UserAlreadyDeletedException(userId);
        }

        user.setDeletedAt(LocalDateTime.now());
        user.setEnabled(false);
        user.setAccountLocked(true);
        jpaUserRepository.save(user);
    }

    // ---------------------------------------------------------------
    // UTILIDADES PRIVADAS
    // ---------------------------------------------------------------

    private UserEntity findUserOrThrow(Long userId) {
        return jpaUserRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    private AdminUserResponseDTO toResponseDTO(UserEntity user) {
        return new AdminUserResponseDTO(
                user.getId(),
                user.getName(),
                user.getDni(),
                user.getEmail(),
                user.getPhone(),
                user.getRole(),
                user.isEnabled(),
                user.isAccountLocked(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getDeletedAt()
        );
    }
}
