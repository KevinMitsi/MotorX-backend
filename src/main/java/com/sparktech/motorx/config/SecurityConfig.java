package com.sparktech.motorx.config;

import com.sparktech.motorx.security.CustomUserDetailsService;
import com.sparktech.motorx.security.JwtAuthenticationFilter;
import com.sparktech.motorx.metrics.MetricsAccessDeniedHandler;
import com.sparktech.motorx.metrics.MetricsAuthenticationEntryPoint;
import com.sparktech.motorx.metrics.PerformanceMetricsFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.cors.CorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    public static final String ADMIN_ROLE = "ADMIN";
    public static final String WAREHOUSE_ROLE = "WAREHOUSE_WORKER";
    public static final String RECEPTIONIST_ROLE = "RECEPTIONIST";
    public static final String TECHNICIAN_ROLE = "TECHNICIAN";
    public static final String SPARES_ROUTE = "/api/v1/spares/**";
    private final CustomUserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final PerformanceMetricsFilter performanceMetricsFilter;
    private final MetricsAuthenticationEntryPoint metricsAuthenticationEntryPoint;
    private final MetricsAccessDeniedHandler metricsAccessDeniedHandler;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider authenticationProvider) {
        // Evita managers observables sin delegado en tiempo de ejecución.
        return new ProviderManager(authenticationProvider);
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(List.of(
                "http://localhost:4200",
                "https://motorx-cf34d.web.app"
        ));
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(List.of("Authorization", "Cache-Control", "Content-Type"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, AuthenticationProvider authenticationProvider) {
        http.csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .authorizeHttpRequests(auth -> auth

                        .requestMatchers("/api/auth/**").permitAll()

                        // Permitir PUT en /api/password-reset y /api/password-reset/ (con y sin barra)
                        .requestMatchers(HttpMethod.PUT, "/api/password-reset").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/password-reset/").permitAll()
                        .requestMatchers("/api/password-reset/**").permitAll()


                        .requestMatchers("/api/public/**").permitAll()
                        .requestMatchers(
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        //Admin users
                        .requestMatchers("api/v1/admin/appointments/agenda").hasAnyRole(ADMIN_ROLE, RECEPTIONIST_ROLE)
                        .requestMatchers("/api/v1/admin/**").hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/spares").hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE)
                        .requestMatchers(HttpMethod.GET, SPARES_ROUTE).hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE, RECEPTIONIST_ROLE)
                        .requestMatchers(HttpMethod.PUT, SPARES_ROUTE).hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE)
                        .requestMatchers(HttpMethod.PATCH, SPARES_ROUTE).hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE)
                        .requestMatchers(HttpMethod.DELETE, SPARES_ROUTE).hasRole(ADMIN_ROLE)

                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/purchases").hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE)
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/purchases/**").hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/inventory/sales").hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE, RECEPTIONIST_ROLE)
                        .requestMatchers(HttpMethod.GET, "/api/v1/inventory/sales/**").hasAnyRole(ADMIN_ROLE, WAREHOUSE_ROLE, RECEPTIONIST_ROLE)

                        .requestMatchers("/api/v1/reception/**").hasAnyRole(ADMIN_ROLE, RECEPTIONIST_ROLE)
                        .requestMatchers("/api/v1/orders/**").hasAnyRole(ADMIN_ROLE, TECHNICIAN_ROLE)
                        .requestMatchers(HttpMethod.GET, "/api/v1/procedures/**").hasAnyRole(ADMIN_ROLE, TECHNICIAN_ROLE)
                        .requestMatchers(HttpMethod.POST, "/api/v1/procedures").hasRole(ADMIN_ROLE)
                        .requestMatchers(HttpMethod.PUT, "/api/v1/procedures/**").hasRole(ADMIN_ROLE)
                        .requestMatchers("/api/v1/services/**").hasRole(ADMIN_ROLE)
                        .requestMatchers("/api/v1/notifications/**").authenticated()
                        .requestMatchers("/api/v1/user/**").authenticated()

                        //end-points health
                        .requestMatchers("/actuator/health").permitAll()
                        // Cualquier otra petición requiere autenticación
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(metricsAuthenticationEntryPoint)
                        .accessDeniedHandler(metricsAccessDeniedHandler)
                )
                .authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .addFilterAfter(performanceMetricsFilter, JwtAuthenticationFilter.class);

        return http.build();
    }
}