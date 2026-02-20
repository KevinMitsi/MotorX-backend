package com.sparktech.motorx.security;
import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.repository.JpaUserRepository;
import lombok.RequiredArgsConstructor;


import org.jetbrains.annotations.NotNull;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;


@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final JpaUserRepository jpaUserRepository;

    @Override
    public @NotNull UserDetails loadUserByUsername(@NotNull String email) throws UsernameNotFoundException {
        UserEntity user = jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));

        // Verificar que el usuario esté habilitado y no eliminado
        if (!user.isEnabled()) {
            throw new UsernameNotFoundException("Usuario deshabilitado o eliminado: " + email);
        }

        return User.builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(user.getAuthorities())
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(false)
                .build();
    }

    public UserEntity getUserEntityByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado con email: " + email));
    }
}