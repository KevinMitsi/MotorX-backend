package com.sparktech.motorx.config;

import com.sparktech.motorx.metrics.MetricsAccessDeniedHandler;
import com.sparktech.motorx.metrics.MetricsAuthenticationEntryPoint;
import com.sparktech.motorx.metrics.PerformanceMetricsFilter;
import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig - Unit Tests")
class SecurityConfigTest {

    @Test
    @DisplayName("passwordEncoder retorna BCryptPasswordEncoder")
    void shouldCreatePasswordEncoder() {
        SecurityConfig config = new SecurityConfig(
                mock(CustomUserDetailsService.class),
                mock(JwtAuthenticationFilter.class),
                mock(PerformanceMetricsFilter.class),
                mock(MetricsAuthenticationEntryPoint.class),
                mock(MetricsAccessDeniedHandler.class)
        );

        assertThat(config.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("authenticationManager delega a AuthenticationConfiguration")
    void shouldGetAuthenticationManagerFromConfiguration() {
        AuthenticationConfiguration authenticationConfiguration = mock(AuthenticationConfiguration.class);
        AuthenticationManager authenticationManager = mock(AuthenticationManager.class);
        when(authenticationConfiguration.getAuthenticationManager()).thenReturn(authenticationManager);

        SecurityConfig config = new SecurityConfig(
                mock(CustomUserDetailsService.class),
                mock(JwtAuthenticationFilter.class),
                mock(PerformanceMetricsFilter.class),
                mock(MetricsAuthenticationEntryPoint.class),
                mock(MetricsAccessDeniedHandler.class)
        );

        AuthenticationManager result = config.authenticationManager(authenticationConfiguration);

        assertThat(result).isSameAs(authenticationManager);
    }

    @Test
    @DisplayName("filterChain agrega handlers y filtros esperados")
    void shouldConfigureFilterChainWithMetricsComponents() {
        CustomUserDetailsService userDetailsService = mock(CustomUserDetailsService.class);
        JwtAuthenticationFilter jwtFilter = mock(JwtAuthenticationFilter.class);
        PerformanceMetricsFilter performanceFilter = mock(PerformanceMetricsFilter.class);
        MetricsAuthenticationEntryPoint entryPoint = mock(MetricsAuthenticationEntryPoint.class);
        MetricsAccessDeniedHandler accessDeniedHandler = mock(MetricsAccessDeniedHandler.class);

        SecurityConfig config = new SecurityConfig(
                userDetailsService,
                jwtFilter,
                performanceFilter,
                entryPoint,
                accessDeniedHandler
        );

        HttpSecurity http = mock(HttpSecurity.class, RETURNS_SELF);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain chain = config.filterChain(http);

        assertThat(chain).isSameAs(expectedChain);
        verify(http).exceptionHandling(any());
        verify(http).addFilterBefore(performanceFilter, JwtAuthenticationFilter.class);
        verify(http).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
    }
}

