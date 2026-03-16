package com.project.domain.notification.infra.messaging;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.dto.EventEnvelope;
import com.dabom.messaging.kafka.event.dto.notification.NotificationPayload;
import com.fasterxml.jackson.core.type.TypeReference;
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
        kafkaEventMessageSupport.consumeByEventType(
                consumerRecord,
                KafkaEventTypes.NOTIFICATION,
                new TypeReference<EventEnvelope<NotificationPayload>>() {},
                (envelope, key) -> {
                    log.info(
                            "Notification 수신 type={}, familyId={}",
                            envelope.payload().type(),
                            envelope.payload().familyId());
                    notificationService.handleNotificationEvent(
                            envelope.payload(), envelope.timestamp());
                });
    }
}
