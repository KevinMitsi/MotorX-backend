package com.sparktech.motorx.Services.impl;

import com.sparktech.motorx.Services.ICurrentUserService;
import com.sparktech.motorx.Services.ILogService;
import com.sparktech.motorx.dto.notification.CreateNotificationDTO;
import com.sparktech.motorx.dto.notification.NotificationResponseDTO;
import com.sparktech.motorx.entity.*;
import com.sparktech.motorx.exception.NotificationNotFoundException;
import com.sparktech.motorx.exception.UserNotFoundException;
import com.sparktech.motorx.repository.JpaNotificationRepository;
import com.sparktech.motorx.repository.JpaUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl - Unit Tests")
class NotificationServiceImplTest {

    @Mock
    private JpaNotificationRepository notificationRepository;
    @Mock
    private JpaUserRepository userRepository;
    @Mock
    private ICurrentUserService currentUserService;
    @Mock
    private ILogService logService;

    @InjectMocks
    private NotificationServiceImpl sut;

    private UserEntity admin;
    private UserEntity client;

    @BeforeEach
    void setUp() {
        admin = user(1L, "admin@test.com", Role.ADMIN);
        client = user(2L, "client@test.com", Role.CLIENT);
        lenient().when(currentUserService.getAuthenticatedUser()).thenReturn(admin);
    }

    @Test
    @DisplayName("createNotification guarda notificacion y retorna DTO")
    void createNotificationShouldSaveAndReturnDto() {
        CreateNotificationDTO dto = new CreateNotificationDTO(2L, "Titulo", "Mensaje", NotificationUrgency.HIGH, "INVENTORY");
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(notificationRepository.save(any(NotificationEntity.class))).thenAnswer(inv -> {
            NotificationEntity n = inv.getArgument(0);
            n.setId(10L);
            n.setCreatedAt(LocalDateTime.now());
            return n;
        });

        NotificationResponseDTO result = sut.createNotification(dto);

        assertThat(result.id()).isEqualTo(10L);
        assertThat(result.userId()).isEqualTo(2L);
        assertThat(result.urgency()).isEqualTo(NotificationUrgency.HIGH);
        verify(logService).logSuccess(eq(LogServiceName.NOTIFICATION), eq(LogActionType.CREATE_NOTIFICATION), eq("admin@test.com"), eq(1L), contains("Notificacion creada"));
    }

    @Test
    @DisplayName("createNotification falla cuando no existe usuario destino")
    void createNotificationShouldFailWhenTargetUserMissing() {
        CreateNotificationDTO dto = new CreateNotificationDTO(99L, "Titulo", "Mensaje", NotificationUrgency.MEDIUM, "SYSTEM");
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.createNotification(dto))
                .isInstanceOf(UserNotFoundException.class);

        verify(logService).logFailure(eq(LogServiceName.NOTIFICATION), eq(LogActionType.CREATE_NOTIFICATION), eq("admin@test.com"), eq(1L), contains("usuario"));
    }

    @Test
    @DisplayName("getMyNotifications con onlyUnread=true retorna solo no leidas")
    void getMyNotificationsShouldReturnOnlyUnread() {
        NotificationEntity unread = notification(30L, admin, false);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(unread));

        List<NotificationResponseDTO> result = sut.getMyNotifications(true);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().isRead()).isFalse();
    }

    @Test
    @DisplayName("getNotificationsByUserId valida existencia de usuario")
    void getNotificationsByUserIdShouldValidateUser() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(client));
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(2L)).thenReturn(List.of(notification(31L, client, true)));

        List<NotificationResponseDTO> result = sut.getNotificationsByUserId(2L);

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().userId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("markAsRead marca y persiste notificacion")
    void markAsReadShouldUpdateNotification() {
        NotificationEntity unread = notification(55L, admin, false);
        when(notificationRepository.findByIdAndUserId(55L, 1L)).thenReturn(Optional.of(unread));
        when(notificationRepository.save(unread)).thenReturn(unread);

        NotificationResponseDTO result = sut.markAsRead(55L);

        assertThat(result.isRead()).isTrue();
        assertThat(unread.getReadAt()).isNotNull();
        verify(logService).logSuccess(eq(LogServiceName.NOTIFICATION), eq(LogActionType.READ_NOTIFICATION), eq("admin@test.com"), eq(1L), contains("55"));
    }

    @Test
    @DisplayName("markAsRead falla con NotificationNotFoundException")
    void markAsReadShouldFailWhenMissing() {
        when(notificationRepository.findByIdAndUserId(70L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> sut.markAsRead(70L))
                .isInstanceOf(NotificationNotFoundException.class);

        verify(logService).logFailure(eq(LogServiceName.NOTIFICATION), eq(LogActionType.READ_NOTIFICATION), eq("admin@test.com"), eq(1L), contains("70"));
    }

    @Test
    @DisplayName("markAllAsRead marca todas las pendientes")
    void markAllAsReadShouldUpdateAllUnread() {
        NotificationEntity n1 = notification(80L, admin, false);
        NotificationEntity n2 = notification(81L, admin, false);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of(n1, n2));

        long updated = sut.markAllAsRead();

        assertThat(updated).isEqualTo(2);
        assertThat(n1.getIsRead()).isTrue();
        assertThat(n2.getIsRead()).isTrue();
        verify(notificationRepository).saveAll(argThat(list -> {
            int count = 0;
            for (NotificationEntity ignored : list) {
                count++;
            }
            return count == 2;
        }));
    }

    @Test
    @DisplayName("markAllAsRead retorna 0 si no hay pendientes")
    void markAllAsReadShouldReturnZeroWhenNoUnread() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L)).thenReturn(List.of());

        long updated = sut.markAllAsRead();

        assertThat(updated).isZero();
        verify(notificationRepository, never()).saveAll(anyList());
    }

    private UserEntity user(Long id, String email, Role role) {
        UserEntity user = new UserEntity();
        user.setId(id);
        user.setEmail(email);
        user.setRole(role);
        return user;
    }

    private NotificationEntity notification(Long id, UserEntity owner, boolean read) {
        NotificationEntity entity = new NotificationEntity();
        entity.setId(id);
        entity.setUser(owner);
        entity.setTitle("Titulo " + id);
        entity.setDescription("Descripcion " + id);
        entity.setUrgency(NotificationUrgency.MEDIUM);
        entity.setIsRead(read);
        entity.setCreatedAt(LocalDateTime.now());
        entity.setSource("SYSTEM");
        return entity;
    }
}

