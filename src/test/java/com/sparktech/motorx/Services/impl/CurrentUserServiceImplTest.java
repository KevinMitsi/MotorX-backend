package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.entity.UserEntity;
import com.sparktech.motorx.repository.JpaUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceImplTest {

    @Mock
    private JpaUserRepository jpaUserRepository;

    private CurrentUserServiceImpl currentUserService;

    @BeforeEach
    void setUp() {
        currentUserService = new CurrentUserServiceImpl(jpaUserRepository);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getAuthenticatedUser_whenPrincipalIsUserEntity_returnsThatUser() {
        UserEntity user = new UserEntity();
        user.setId(1L);
        user.setEmail("user@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(user);

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        UserEntity result = currentUserService.getAuthenticatedUser();

        assertSame(user, result);
        verifyNoInteractions(jpaUserRepository);
    }

    @Test
    void getAuthenticatedUser_whenPrincipalIsNotUserEntity_looksUpByEmail() {
        UserEntity user = new UserEntity();
        user.setId(2L);
        user.setEmail("found@example.com");

        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new Object());
        when(authentication.getName()).thenReturn("found@example.com");

        when(jpaUserRepository.findByEmail("found@example.com")).thenReturn(Optional.of(user));

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        UserEntity result = currentUserService.getAuthenticatedUser();

        assertNotNull(result);
        assertEquals(2L, result.getId());
        assertEquals("found@example.com", result.getEmail());
        verify(jpaUserRepository).findByEmail("found@example.com");
    }

    @Test
    void getAuthenticatedUser_whenNoAuthentication_throwsIllegalStateException() {
        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(null);
        SecurityContextHolder.setContext(context);

        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                currentUserService.getAuthenticatedUser()
        );

        assertEquals("No hay usuario autenticado.", ex.getMessage());
    }

    @Test
    void getAuthenticatedUser_whenEmailNotFound_throwsIllegalArgumentException() {
        Authentication authentication = mock(Authentication.class);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(new Object());
        when(authentication.getName()).thenReturn("missing@example.com");

        when(jpaUserRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        SecurityContext context = mock(SecurityContext.class);
        when(context.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(context);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                currentUserService.getAuthenticatedUser()
        );

        assertTrue(ex.getMessage().contains("missing@example.com"));
    }

}