package com.project.domain.usagerecord.infra.messaging;

import java.time.LocalDateTime;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.dabom.messaging.kafka.contract.KafkaEventTypes;
import com.dabom.messaging.kafka.contract.KafkaTopics;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.dto.EventEnvelope;
import com.dabom.messaging.kafka.event.dto.usage.UsageRealtimePayload;
import com.project.domain.usagerecord.infra.messaging.config.UsageRealtimeBroadcastKafkaConfig;
import com.project.domain.usagerecord.infra.sse.SsePublisher;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class UsageRecordKafkaConsumer {

    private final KafkaEventMessageSupport kafkaEventMessageSupport;
    private final SsePublisher ssePublisher;

    @KafkaListener(
            topics = KafkaTopics.USAGE_REALTIME,
            containerFactory =
                    UsageRealtimeBroadcastKafkaConfig
                            .USAGE_REALTIME_BROADCAST_KAFKA_LISTENER_CONTAINER_FACTORY)
    public void consume(ConsumerRecord<String, String> record) {
        kafkaEventMessageSupport.consumeByEventType(
                record,
                KafkaEventTypes.USAGE_REALTIME,
                new TypeReference<EventEnvelope<UsageRealtimePayload>>() {},
                (envelope, key) -> {
                    UsageRealtimePayload payload = envelope.payload();
                    LocalDateTime publishTime = envelope.timestamp();

                    log.info(
                            "FamilyId:{}, totalUsedBytes:{}",
                            payload.familyId(),
                            payload.totalUsedBytes());

                    ssePublisher.pushMemberUsageBytes(payload, publishTime);
                    ssePublisher.pushTotalUsageBytes(payload, publishTime);
                });
    }
}
