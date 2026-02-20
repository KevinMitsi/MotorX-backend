package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IUserService;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.entity.Role;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final JpaUserRepository jpaUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void register(RegisterUserDTO request) {
        if (jpaUserRepository.existsByEmail(request.email())) {
            throw new IllegalArgumentException("El email ya está registrado");
        }
        if (jpaUserRepository.existsByDni(request.dni())) {
            throw new IllegalArgumentException("El DNI ya está registrado");
        }

        String encodedPassword = passwordEncoder.encode(request.password());

        UserEntity user = new UserEntity();
        user.setName(request.name());
        user.setDni(request.dni());
        user.setEmail(request.email());
        user.setPassword(encodedPassword);
        user.setPhone(request.phone());
        user.setRole(Role.CLIENT);
        user.setEnabled(true);
        user.setAccountLocked(false);


        jpaUserRepository.save(user);
    }
}

