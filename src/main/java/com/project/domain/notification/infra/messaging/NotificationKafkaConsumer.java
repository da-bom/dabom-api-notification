package com.project.domain.notification.infra.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.dto.EventEnvelope;
import com.dabom.messaging.kafka.event.dto.notification.CustomerBlockedPayload;
import com.dabom.messaging.kafka.event.dto.notification.NotificationSubTypes;
import com.dabom.messaging.kafka.event.dto.notification.ThresholdAlertPayload;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.project.domain.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NotificationKafkaConsumer {

    private final KafkaEventMessageSupport kafkaEventMessageSupport;
    private final NotificationService notificationService;

    @KafkaListener(topics = KafkaTopics.NOTIFICATION)
    public void consume(ConsumerRecord<String, String> consumerRecord) {
        try {
            JsonNode tree = kafkaEventMessageSupport.readTree(consumerRecord.value());
            String eventType = kafkaEventMessageSupport.extractEventType(tree);

            if (!KafkaEventTypes.NOTIFICATION.equals(eventType)) {
                return;
            }

            String subType = tree.path("subType").asText();
            dispatch(tree, subType);
        } catch (JsonProcessingException e) {
            log.error("Notification 이벤트 파싱 실패: {}", e.getMessage());
        }
    }

    private void dispatch(JsonNode tree, String subType) {
        switch (subType) {
            case NotificationSubTypes.THRESHOLD_ALERT -> {
                EventEnvelope<ThresholdAlertPayload> envelope =
                        kafkaEventMessageSupport.convertToEnvelope(
                                tree, new TypeReference<EventEnvelope<ThresholdAlertPayload>>() {});
                log.info(
                        "ThresholdAlert 수신 familyId={}, threshold={}%",
                        envelope.payload().familyId(), envelope.payload().thresholdPercent());
                notificationService.handleThresholdAlert(envelope.payload(), envelope.timestamp());
            }
            case NotificationSubTypes.CUSTOMER_BLOCKED -> {
                EventEnvelope<CustomerBlockedPayload> envelope =
                        kafkaEventMessageSupport.convertToEnvelope(
                                tree,
                                new TypeReference<EventEnvelope<CustomerBlockedPayload>>() {});
                log.info(
                        "CustomerBlocked 수신 customerId={}, familyId={}",
                        envelope.payload().customerId(),
                        envelope.payload().familyId());
                notificationService.handleCustomerBlocked(envelope.payload(), envelope.timestamp());
            }
        }
    }
}
