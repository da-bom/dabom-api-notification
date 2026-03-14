package com.project.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dabom.messaging.kafka.event.dto.notification.CustomerBlockedPayload;
import com.dabom.messaging.kafka.event.dto.notification.ThresholdAlertPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.entity.NotificationType;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.domain.webpush.service.WebPushService;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 단위 테스트")
class NotificationServiceImplTest {

    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private WebPushService webPushService;
    @Mock private ObjectMapper objectMapper;

    @InjectMocks private NotificationServiceImpl notificationService;

    @Captor private ArgumentCaptor<List<NotificationLog>> logsCaptor;
    @Captor private ArgumentCaptor<NotificationLog> logCaptor;

    private static final Long FAMILY_ID = 1L;
    private static final Long CUSTOMER_ID_1 = 10L;
    private static final Long CUSTOMER_ID_2 = 11L;
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 3, 14, 12, 0, 0);

    @Nested
    @DisplayName("handleThresholdAlert")
    class HandleThresholdAlert {

        @Test
        @DisplayName("가족 구성원 수만큼 NotificationLog 생성 후 sendToFamily 호출")
        void createsLogsForAllFamilyMembers() {
            ThresholdAlertPayload payload = new ThresholdAlertPayload(FAMILY_ID, 50, "데이터 50% 사용");
            when(familyMemberRepository.findCustomerIdsByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(CUSTOMER_ID_1, CUSTOMER_ID_2));

            notificationService.handleThresholdAlert(payload, SENT_AT);

            verify(notificationLogRepository).saveAll(logsCaptor.capture());
            List<NotificationLog> saved = logsCaptor.getValue();
            assertThat(saved).hasSize(2);
            assertThat(saved)
                    .allSatisfy(
                            log -> {
                                assertThat(log.getFamilyId()).isEqualTo(FAMILY_ID);
                                assertThat(log.getType())
                                        .isEqualTo(NotificationType.THRESHOLD_ALERT);
                                assertThat(log.getMessage()).isEqualTo("데이터 50% 사용");
                                assertThat(log.getSentAt()).isEqualTo(SENT_AT);
                                assertThat(log.isRead()).isFalse();
                            });
            assertThat(saved.get(0).getCustomerId()).isEqualTo(CUSTOMER_ID_1);
            assertThat(saved.get(1).getCustomerId()).isEqualTo(CUSTOMER_ID_2);
            verify(webPushService).sendToFamily(FAMILY_ID, "데이터 50% 사용");
        }

        @Test
        @DisplayName("가족 구성원이 없으면 빈 리스트 저장, 푸시는 호출")
        void emptyFamilyMembers_savesEmptyList() {
            ThresholdAlertPayload payload = new ThresholdAlertPayload(FAMILY_ID, 50, "데이터 50% 사용");
            when(familyMemberRepository.findCustomerIdsByFamilyId(FAMILY_ID)).thenReturn(List.of());

            notificationService.handleThresholdAlert(payload, SENT_AT);

            verify(notificationLogRepository).saveAll(logsCaptor.capture());
            assertThat(logsCaptor.getValue()).isEmpty();
            verify(webPushService).sendToFamily(FAMILY_ID, "데이터 50% 사용");
        }

        @Test
        @DisplayName("푸시 전송 실패 시 예외 전파 없이 정상 완료")
        void pushFailure_doesNotPropagate() {
            ThresholdAlertPayload payload = new ThresholdAlertPayload(FAMILY_ID, 50, "데이터 50% 사용");
            when(familyMemberRepository.findCustomerIdsByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(CUSTOMER_ID_1));
            doThrow(new RuntimeException("push failed"))
                    .when(webPushService)
                    .sendToFamily(anyLong(), anyString());

            assertThatCode(() -> notificationService.handleThresholdAlert(payload, SENT_AT))
                    .doesNotThrowAnyException();

            verify(notificationLogRepository).saveAll(any());
        }
    }

    @Nested
    @DisplayName("handleCustomerBlocked")
    class HandleCustomerBlocked {

        @Test
        @DisplayName("차단된 고객에게 NotificationLog 1건 생성 후 sendToUser 호출")
        void createsLogAndSendsPush() {
            CustomerBlockedPayload payload =
                    new CustomerBlockedPayload(
                            FAMILY_ID, CUSTOMER_ID_1, "MONTHLY_LIMIT_EXCEEDED", "2026-03-14T12:00");

            notificationService.handleCustomerBlocked(payload, SENT_AT);

            verify(notificationLogRepository).save(logCaptor.capture());
            NotificationLog saved = logCaptor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID_1);
            assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
            assertThat(saved.getType()).isEqualTo(NotificationType.BLOCKED);
            assertThat(saved.getMessage()).contains("MONTHLY_LIMIT_EXCEEDED");
            assertThat(saved.getSentAt()).isEqualTo(SENT_AT);
            assertThat(saved.isRead()).isFalse();
            verify(webPushService).sendToUser(eq(CUSTOMER_ID_1), anyString());
        }

        @Test
        @DisplayName("푸시 전송 실패 시 예외 전파 없이 정상 완료")
        void pushFailure_doesNotPropagate() {
            CustomerBlockedPayload payload =
                    new CustomerBlockedPayload(
                            FAMILY_ID, CUSTOMER_ID_1, "MONTHLY_LIMIT_EXCEEDED", "2026-03-14T12:00");
            doThrow(new RuntimeException("subscription not found"))
                    .when(webPushService)
                    .sendToUser(anyLong(), anyString());

            assertThatCode(() -> notificationService.handleCustomerBlocked(payload, SENT_AT))
                    .doesNotThrowAnyException();

            verify(notificationLogRepository).save(any(NotificationLog.class));
        }
    }
}
