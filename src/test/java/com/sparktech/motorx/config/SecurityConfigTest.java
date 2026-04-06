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
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SecurityConfig - Unit Tests")
class SecurityConfigTest {

    private SecurityConfig buildConfig() {
        return new SecurityConfig(
                mock(CustomUserDetailsService.class),
                mock(JwtAuthenticationFilter.class),
                mock(PerformanceMetricsFilter.class),
                mock(MetricsAuthenticationEntryPoint.class),
                mock(MetricsAccessDeniedHandler.class)
        );
    }

    @Test
    @DisplayName("passwordEncoder retorna BCryptPasswordEncoder")
    void shouldCreatePasswordEncoder() {
        SecurityConfig config = buildConfig();

        assertThat(config.passwordEncoder()).isInstanceOf(BCryptPasswordEncoder.class);
    }

    @Test
    @DisplayName("authenticationManager usa AuthenticationProvider configurado")
    void shouldCreateAuthenticationManagerFromProvider() {
        AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);
        var request = new org.springframework.security.authentication.UsernamePasswordAuthenticationToken("user@test.com", "pass123");
        var expected = mock(org.springframework.security.core.Authentication.class);

        when(authenticationProvider.supports(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)).thenReturn(true);
        when(authenticationProvider.authenticate(request)).thenReturn(expected);

        SecurityConfig config = buildConfig();

        AuthenticationManager result = config.authenticationManager(authenticationProvider);

        assertThat(result.authenticate(request)).isSameAs(expected);
        verify(authenticationProvider).authenticate(request);
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
        AuthenticationProvider authenticationProvider = mock(AuthenticationProvider.class);
        DefaultSecurityFilterChain expectedChain = mock(DefaultSecurityFilterChain.class);
        when(http.build()).thenReturn(expectedChain);

        SecurityFilterChain chain = config.filterChain(http, authenticationProvider);

        assertThat(chain).isSameAs(expectedChain);
        verify(http).exceptionHandling(any());
        verify(http).authenticationProvider(authenticationProvider);
        verify(http).addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);
        verify(http).addFilterAfter(performanceFilter, JwtAuthenticationFilter.class);
    }

    @Test
    @DisplayName("authenticationProvider retorna DaoAuthenticationProvider")
    void shouldCreateDaoAuthenticationProvider() {
        SecurityConfig config = buildConfig();

        AuthenticationProvider provider = config.authenticationProvider();

        assertThat(provider).isInstanceOf(DaoAuthenticationProvider.class);
    }

    @Test
    @DisplayName("corsConfigurationSource configura orígenes y métodos")
    void shouldCreateCorsConfigurationSource() {
        SecurityConfig config = buildConfig();

        CorsConfigurationSource source = config.corsConfigurationSource();

        assertThat(source).isInstanceOf(UrlBasedCorsConfigurationSource.class);
        UrlBasedCorsConfigurationSource urlSource = (UrlBasedCorsConfigurationSource) source;
        var configuration = urlSource.getCorsConfigurations().get("/**");

        assertThat(configuration).isNotNull();
        assertThat(configuration.getAllowedOrigins()).contains("http://localhost:4200", "https://motorx-cf34d.web.app");
        assertThat(configuration.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH");
        assertThat(configuration.getAllowCredentials()).isTrue();
    }
}

