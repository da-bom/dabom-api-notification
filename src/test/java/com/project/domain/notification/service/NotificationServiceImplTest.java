package com.project.domain.notification.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.dabom.messaging.kafka.event.dto.notification.NotificationType;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.domain.usagerecord.infra.sse.SsePublisher;
import com.project.domain.webpush.service.WebPushService;
import com.project.global.exception.ApplicationException;
import com.project.global.util.CursorUtil;

@ExtendWith(MockitoExtension.class)
@DisplayName("NotificationServiceImpl 단위 테스트")
class NotificationServiceImplTest {

    @Mock private NotificationLogRepository notificationLogRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @Mock private WebPushService webPushService;
    @Mock private SsePublisher ssePublisher;
    @Mock private ObjectMapper objectMapper;
    @Mock private CursorUtil cursorUtil;

    @InjectMocks private NotificationServiceImpl notificationService;

    @Captor private ArgumentCaptor<NotificationLog> logCaptor;

    private static final Long FAMILY_ID = 1L;
    private static final Long CUSTOMER_ID_1 = 10L;
    private static final Long CUSTOMER_ID_2 = 11L;
    private static final LocalDateTime SENT_AT = LocalDateTime.of(2026, 3, 14, 12, 0, 0);

    @Nested
    @DisplayName("handleNotificationEvent - 가족 전체 fan-out (customerId == null)")
    class HandleFamilyFanOut {

        @Test
        @DisplayName("가족 구성원 수만큼 NotificationLog 생성 후 sendToFamily 호출")
        void createsLogsForAllFamilyMembers() throws JsonProcessingException {
            NotificationPayload payload =
                    new NotificationPayload(
                            FAMILY_ID,
                            null,
                            NotificationType.THRESHOLD_ALERT,
                            "데이터 경고",
                            "데이터 50% 사용",
                            Map.of("thresholdPercent", 50));
            when(familyMemberRepository.findCustomerIdsByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(CUSTOMER_ID_1, CUSTOMER_ID_2));
            when(objectMapper.writeValueAsString(payload.data()))
                    .thenReturn("{\"thresholdPercent\":50}");

            notificationService.handleNotificationEvent(payload, SENT_AT);

            verify(notificationLogRepository, times(2)).save(logCaptor.capture());
            List<NotificationLog> allSaved = logCaptor.getAllValues();
            assertThat(allSaved).hasSize(2);
            assertThat(allSaved)
                    .allSatisfy(
                            saved -> {
                                assertThat(saved.getFamilyId()).isEqualTo(FAMILY_ID);
                                assertThat(saved.getType())
                                        .isEqualTo(NotificationType.THRESHOLD_ALERT);
                                assertThat(saved.getTitle()).isEqualTo("데이터 경고");
                                assertThat(saved.getMessage()).isEqualTo("데이터 50% 사용");
                            });
            verify(webPushService).sendToFamily(FAMILY_ID, "데이터 경고", "데이터 50% 사용");
            verify(ssePublisher).pushNotificationEvent(eq(FAMILY_ID), eq("THRESHOLD_ALERT"), any());
        }

        @Test
        @DisplayName("푸시 전송 실패 시 예외 전파 없이 정상 완료")
        void pushFailure_doesNotPropagate() throws JsonProcessingException {
            NotificationPayload payload =
                    new NotificationPayload(
                            FAMILY_ID,
                            null,
                            NotificationType.THRESHOLD_ALERT,
                            "데이터 경고",
                            "데이터 50% 사용",
                            Map.of());
            when(familyMemberRepository.findCustomerIdsByFamilyId(FAMILY_ID))
                    .thenReturn(List.of(CUSTOMER_ID_1));
            when(objectMapper.writeValueAsString(payload.data())).thenReturn("{}");
            doThrow(new RuntimeException("push failed"))
                    .when(webPushService)
                    .sendToFamily(anyLong(), anyString(), anyString());

            notificationService.handleNotificationEvent(payload, SENT_AT);

            verify(notificationLogRepository).save(any(NotificationLog.class));
        }
    }

    @Nested
    @DisplayName("handleNotificationEvent - 단일 고객 (customerId != null)")
    class HandleSingleCustomer {

        @Test
        @DisplayName("단일 고객에게 NotificationLog 1건 생성 후 sendToUser 호출")
        void createsLogAndSendsPush() throws JsonProcessingException {
            NotificationPayload payload =
                    new NotificationPayload(
                            FAMILY_ID,
                            CUSTOMER_ID_1,
                            NotificationType.CUSTOMER_BLOCKED,
                            "데이터 차단",
                            "데이터 사용이 차단되었습니다.",
                            Map.of("blockReason", "MONTHLY_LIMIT_EXCEEDED"));
            when(objectMapper.writeValueAsString(payload.data()))
                    .thenReturn("{\"blockReason\":\"MONTHLY_LIMIT_EXCEEDED\"}");

            notificationService.handleNotificationEvent(payload, SENT_AT);

            verify(notificationLogRepository).save(logCaptor.capture());
            NotificationLog saved = logCaptor.getValue();
            assertThat(saved.getCustomerId()).isEqualTo(CUSTOMER_ID_1);
            assertThat(saved.getType()).isEqualTo(NotificationType.CUSTOMER_BLOCKED);
            assertThat(saved.getTitle()).isEqualTo("데이터 차단");
            verify(webPushService).sendToUser(eq(CUSTOMER_ID_1), anyString(), anyString());
        }

        @Test
        @DisplayName("푸시 전송 실패 시 예외 전파 없이 정상 완료")
        void pushFailure_doesNotPropagate() throws JsonProcessingException {
            NotificationPayload payload =
                    new NotificationPayload(
                            FAMILY_ID,
                            CUSTOMER_ID_1,
                            NotificationType.CUSTOMER_BLOCKED,
                            "데이터 차단",
                            "차단됨",
                            Map.of());
            when(objectMapper.writeValueAsString(payload.data())).thenReturn("{}");
            doThrow(new RuntimeException("subscription not found"))
                    .when(webPushService)
                    .sendToUser(anyLong(), anyString(), anyString());

            notificationService.handleNotificationEvent(payload, SENT_AT);

            verify(notificationLogRepository).save(any(NotificationLog.class));
        }
    }

    @Nested
    @DisplayName("serializePayload 실패")
    class SerializePayloadFailure {

        @Test
        @DisplayName("직렬화 실패 시 ApplicationException 발생")
        void throwsApplicationException() throws JsonProcessingException {
            NotificationPayload payload =
                    new NotificationPayload(
                            FAMILY_ID,
                            null,
                            NotificationType.THRESHOLD_ALERT,
                            "데이터 경고",
                            "msg",
                            Map.of());
            when(objectMapper.writeValueAsString(payload.data()))
                    .thenThrow(new JsonProcessingException("serialize error") {});

            assertThatThrownBy(() -> notificationService.handleNotificationEvent(payload, SENT_AT))
                    .isInstanceOf(ApplicationException.class);
        }
    }
}
