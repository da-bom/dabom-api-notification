package com.project.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.project.domain.notification.dto.NotificationSlice;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.global.exception.ApplicationException;
import com.project.global.util.CursorUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 단위 테스트")
class NotificationServiceImplTest {

    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private CursorUtil cursorUtil;

    @InjectMocks private NotificationServiceImpl notificationService;

    private static final Long CUSTOMER_ID = 10L;
    private static final Long NOTIFICATION_ID = 100L;
    private static final Long FAMILY_ID = 1L;

    private NotificationLog createNotificationLog(Long id) {
        NotificationLog log =
                NotificationLog.builder()
                        .customerId(CUSTOMER_ID)
                        .familyId(FAMILY_ID)
                        .type(NotificationType.THRESHOLD_ALERT)
                        .title("테스트 알림")
                        .message("테스트 메시지")
                        .payload("{}")
                        .sentAt(LocalDateTime.now())
                        .build();
        try {
            var idField = NotificationLog.class.getDeclaredField("id");
            idField.setAccessible(true);
            idField.set(log, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return log;
    }

    @Nested
    @DisplayName("getNotifications")
    class GetNotifications {

        @Test
        @DisplayName("다음 페이지가 있을 때 hasNext=true, nextCursor 반환")
        void returnsSliceWithNextPage() {
            int size = 2;
            List<NotificationLog> logs = new ArrayList<>();
            logs.add(createNotificationLog(3L));
            logs.add(createNotificationLog(2L));
            logs.add(createNotificationLog(1L));

            when(cursorUtil.decode(null)).thenReturn(null);
            when(notificationLogRepository.findByCustomerIdWithCursor(
                            eq(CUSTOMER_ID), eq(null), eq(size), eq(null), eq(null), any()))
                    .thenReturn(logs);
            when(cursorUtil.encode(2L)).thenReturn("encoded-cursor");
            when(notificationLogRepository.countUnread(eq(CUSTOMER_ID), any())).thenReturn(5L);

            NotificationSlice result =
                    notificationService.getNotifications(CUSTOMER_ID, null, size, null, null);

            assertThat(result.hasNext()).isTrue();
            assertThat(result.nextCursor()).isEqualTo("encoded-cursor");
            assertThat(result.content()).hasSize(2);
            assertThat(result.unreadCount()).isEqualTo(5L);
        }

        @Test
        @DisplayName("다음 페이지가 없을 때 hasNext=false, nextCursor=null")
        void returnsSliceWithoutNextPage() {
            int size = 10;
            List<NotificationLog> logs = List.of(createNotificationLog(1L));

            when(cursorUtil.decode(null)).thenReturn(null);
            when(notificationLogRepository.findByCustomerIdWithCursor(
                            eq(CUSTOMER_ID), eq(null), eq(size), eq(null), eq(null), any()))
                    .thenReturn(logs);
            when(notificationLogRepository.countUnread(eq(CUSTOMER_ID), any())).thenReturn(0L);

            NotificationSlice result =
                    notificationService.getNotifications(CUSTOMER_ID, null, size, null, null);

            assertThat(result.hasNext()).isFalse();
            assertThat(result.nextCursor()).isNull();
            assertThat(result.content()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("getUnreadCount")
    class GetUnreadCount {

        @Test
        @DisplayName("읽지 않은 알림 수 반환")
        void returnsUnreadCount() {
            when(notificationLogRepository.countUnread(eq(CUSTOMER_ID), any())).thenReturn(7L);

            long count = notificationService.getUnreadCount(CUSTOMER_ID);

            assertThat(count).isEqualTo(7L);
        }
    }

    @Nested
    @DisplayName("markAsRead")
    class MarkAsRead {

        @Test
        @DisplayName("알림을 읽음 처리")
        void marksNotificationAsRead() {
            NotificationLog log = createNotificationLog(NOTIFICATION_ID);
            when(notificationLogRepository.findByIdAndCustomerId(NOTIFICATION_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(log));

            notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID);

            assertThat(log.isRead()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 알림이면 예외 발생")
        void throwsWhenNotFound() {
            when(notificationLogRepository.findByIdAndCustomerId(NOTIFICATION_ID, CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID))
                    .isInstanceOf(ApplicationException.class);
        }

        @Test
        @DisplayName("삭제된 알림이면 예외 발생")
        void throwsWhenDeleted() {
            NotificationLog log = createNotificationLog(NOTIFICATION_ID);
            log.softDelete();
            when(notificationLogRepository.findByIdAndCustomerId(NOTIFICATION_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(log));

            assertThatThrownBy(() -> notificationService.markAsRead(NOTIFICATION_ID, CUSTOMER_ID))
                    .isInstanceOf(ApplicationException.class);
        }
    }

    @Nested
    @DisplayName("markAllAsRead")
    class MarkAllAsRead {

        @Test
        @DisplayName("전체 읽음 처리 호출")
        void callsRepositoryMarkAllAsRead() {
            notificationService.markAllAsRead(CUSTOMER_ID);

            verify(notificationLogRepository).markAllAsRead(eq(CUSTOMER_ID), any());
        }
    }

    @Nested
    @DisplayName("deleteNotification")
    class DeleteNotification {

        @Test
        @DisplayName("알림 소프트 삭제")
        void softDeletesNotification() {
            NotificationLog log = createNotificationLog(NOTIFICATION_ID);
            when(notificationLogRepository.findByIdAndCustomerId(NOTIFICATION_ID, CUSTOMER_ID))
                    .thenReturn(Optional.of(log));

            notificationService.deleteNotification(NOTIFICATION_ID, CUSTOMER_ID);

            assertThat(log.isDeleted()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 알림이면 예외 발생")
        void throwsWhenNotFound() {
            when(notificationLogRepository.findByIdAndCustomerId(NOTIFICATION_ID, CUSTOMER_ID))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(
                            () ->
                                    notificationService.deleteNotification(
                                            NOTIFICATION_ID, CUSTOMER_ID))
                    .isInstanceOf(ApplicationException.class);
        }
    }
}
