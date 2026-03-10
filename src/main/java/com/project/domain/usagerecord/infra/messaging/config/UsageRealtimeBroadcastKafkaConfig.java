package com.project.domain.usagerecord.infra.messaging.config;

import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.listener.CommonErrorHandler;

import com.dabom.messaging.kafka.autoconfigure.KafkaConfig;
import com.dabom.messaging.kafka.autoconfigure.KafkaErrorHandlerConfig;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.publisher.DefaultKafkaEventPublisher;
import com.dabom.messaging.kafka.error.KafkaExceptionClassifier;
import com.dabom.messaging.kafka.metrics.KafkaMetrics;
import com.dabom.messaging.kafka.metrics.consumer.KafkaMetricsRecordInterceptor;
import com.dabom.messaging.kafka.metrics.producer.KafkaMetricsProducerListener;
import com.dabom.messaging.kafka.support.KafkaLogSanitizer;

@Configuration
@Import({
    KafkaConfig.class,
    KafkaErrorHandlerConfig.class,
    KafkaMetrics.class,
    KafkaExceptionClassifier.class,
    KafkaLogSanitizer.class,
    KafkaEventMessageSupport.class,
    KafkaMetricsRecordInterceptor.class,
    KafkaMetricsProducerListener.class,
    DefaultKafkaEventPublisher.class
})
public class UsageRealtimeBroadcastKafkaConfig {

    public static final String USAGE_REALTIME_BROADCAST_KAFKA_LISTENER_CONTAINER_FACTORY =
            "usageRealtimeBroadcastKafkaListenerContainerFactory";

    private static final int GROUP_ID_SUFFIX_LENGTH = 8;

    @Bean(USAGE_REALTIME_BROADCAST_KAFKA_LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, String>
            usageRealtimeBroadcastKafkaListenerContainerFactory(
                    ConsumerFactory<String, String> consumerFactory,
                    CommonErrorHandler kafkaCommonErrorHandler,
                    KafkaMetricsRecordInterceptor kafkaMetricsRecordInterceptor,
                    @Value("${spring.kafka.broadcast.group-id-prefix:usage-realtime}")
                            String broadcastGroupIdPrefix) {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory);
        factory.getContainerProperties()
                .setGroupId(
                        broadcastGroupIdPrefix
                                + "-"
                                + UUID.randomUUID()
                                        .toString()
                                        .substring(0, GROUP_ID_SUFFIX_LENGTH));
        factory.getContainerProperties().setObservationEnabled(true);
        factory.setRecordInterceptor(kafkaMetricsRecordInterceptor);
        factory.setCommonErrorHandler(kafkaCommonErrorHandler);
        return factory;
    }
}
