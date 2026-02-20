package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.IAuthService;
import com.sparktech.motorx.Services.IUserService;
import com.sparktech.motorx.dto.auth.AuthResponseDTO;
import com.sparktech.motorx.dto.auth.LoginRequestDTO;
import com.sparktech.motorx.dto.auth.RegisterUserDTO;
import com.sparktech.motorx.dto.user.UserDTO;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.exception.InvalidPasswordException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.mapper.UserEntityMapper;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final IUserService userService;
    private final CustomUserDetailsService userDetailsService;
    private final UserEntityMapper userMapper;

    @Override
    @Transactional
    public AuthResponseDTO login(LoginRequestDTO loginRequest) throws InvalidPasswordException {
        try {
            log.info("Intentando autenticar usuario: {}", loginRequest.email());
            AuthResult result = authenticateAndBuildAuthResult(loginRequest.email(), loginRequest.password());

            String token = result.token;
            UserEntity user = result.user;

            log.info("Usuario autenticado exitosamente: {}", loginRequest.email());

            // Construir respuesta según AuthResponseDTO disponible
            return new AuthResponseDTO(
                    token,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );

        } catch (BadCredentialsException e) {
            log.warn("Credenciales inválidas para: {}", loginRequest.email());
            throw new InvalidPasswordException("Credenciales inválidas");
        }
    }

    @Override
    @Transactional
    public AuthResponseDTO register(RegisterUserDTO registerRequest) {
        log.info("Registrando nuevo usuario: {}", registerRequest.email());
        userService.register(registerRequest);

        AuthResult result = authenticateAndBuildAuthResult(registerRequest.email(), registerRequest.password());

        String token = result.token;
        UserEntity user = result.user;

        log.info("Usuario registrado y autenticado: {}", registerRequest.email());

        return new AuthResponseDTO(
                token,
                user.getId(),
                user.getEmail(),
                user.getName(),
                user.getRole()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public UserDTO getCurrentUser() throws UserNotFoundException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() ||
                Objects.equals(authentication.getPrincipal(), "anonymousUser")) {
            throw new UserNotFoundException("No hay usuario autenticado");
        }

        String email = authentication.getName();
        UserEntity user = userDetailsService.getUserEntityByEmail(email);

        return userMapper.toUserDTO(user);
    }

    @Override
    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("Usuario deslogueado exitosamente");
    }

    @Override
    public AuthResponseDTO refreshToken(String refreshToken) throws InvalidPasswordException {
        try {
            log.info("Intentando renovar token");

            String email = jwtService.extractUsername(refreshToken);
            if (email == null) {
                throw new InvalidPasswordException("Token inválido");
            }

            UserDetails userDetails = userDetailsService.loadUserByUsername(email);
            if (!jwtService.isTokenValid(refreshToken, userDetails)) {
                throw new InvalidPasswordException("Token inválido");
            }

            String newToken = jwtService.generateToken(userDetails);
            UserEntity user = userDetailsService.getUserEntityByEmail(email);

            log.info("Token renovado exitosamente para: {}", email);

            return new AuthResponseDTO(
                    newToken,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
        } catch (Exception e) {
            log.error("Error renovando token: {}", e.getMessage());
            throw new InvalidPasswordException("Token de renovación inválido");
        }
    }

    // Nuevo contenedor privado para devolver token + user
        private record AuthResult(String token, UserEntity user) {
    }

    // Método auxiliar extraído para evitar duplicación
    private AuthResult authenticateAndBuildAuthResult(String email, String password) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );

        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        assert userDetails != null;
        UserEntity user = userDetailsService.getUserEntityByEmail(userDetails.getUsername());

        String token = jwtService.generateToken(userDetails);

        return new AuthResult(token, user);
    }

}