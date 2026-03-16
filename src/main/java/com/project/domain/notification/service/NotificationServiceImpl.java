package com.project.domain.notification.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.dabom.messaging.kafka.event.dto.notification.CustomerBlockedPayload;
import com.dabom.messaging.kafka.event.dto.notification.ThresholdAlertPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.notification.entity.NotificationLog;
import com.project.domain.notification.entity.NotificationType;
import com.project.domain.notification.repository.NotificationLogRepository;
import com.project.domain.webpush.service.WebPushService;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.NotificationErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final String BLOCKED_MESSAGE_FORMAT = "데이터 사용이 차단되었습니다. 사유: %s";

    private final NotificationLogRepository notificationLogRepository;
    private final FamilyMemberRepository familyMemberRepository;
    private final WebPushService webPushService;
    private final ObjectMapper objectMapper;

    @Transactional
    @Override
    public void handleThresholdAlert(ThresholdAlertPayload payload, LocalDateTime sentAt) {
        String payloadJson = serializePayload(payload);
        Long familyId = payload.familyId();
        String message = payload.message();

        List<Long> customerIds = familyMemberRepository.findCustomerIdsByFamilyId(familyId);

        List<NotificationLog> logs =
                customerIds.stream()
                        .map(
                                customerId ->
                                        NotificationLog.builder()
                                                .customerId(customerId)
                                                .familyId(familyId)
                                                .type(NotificationType.THRESHOLD_ALERT)
                                                .message(message)
                                                .payload(payloadJson)
                                                .sentAt(sentAt)
                                                .build())
                        .toList();

        notificationLogRepository.saveAll(logs);

        try {
            webPushService.sendToFamily(familyId, "데이터 경고", message);
        } catch (Exception e) {
            log.warn("ThresholdAlert 푸시 전송 실패 familyId={}", familyId, e);
        }
    }

    @Transactional
    @Override
    public void handleCustomerBlocked(CustomerBlockedPayload payload, LocalDateTime sentAt) {
        String payloadJson = serializePayload(payload);
        Long customerId = payload.customerId();
        Long familyId = payload.familyId();
        String message = String.format(BLOCKED_MESSAGE_FORMAT, payload.blockReason());

        NotificationLog notificationLog =
                NotificationLog.builder()
                        .customerId(customerId)
                        .familyId(familyId)
                        .type(NotificationType.BLOCKED)
                        .message(message)
                        .payload(payloadJson)
                        .sentAt(sentAt)
                        .build();

        notificationLogRepository.save(notificationLog);

        try {
            webPushService.sendToUser(customerId, "데이터 차단", message);
        } catch (Exception e) {
            log.warn("CustomerBlocked 푸시 전송 실패 customerId={}", customerId, e);
        }
    }

    private String serializePayload(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            log.error("Payload 직렬화 실패", e);
            throw new ApplicationException(NotificationErrorCode.NOTIFICATION_SAVE_FAILED);
        }
    }
}
