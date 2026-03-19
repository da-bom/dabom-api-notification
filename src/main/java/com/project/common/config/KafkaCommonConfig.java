package com.project.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import com.dabom.messaging.kafka.autoconfigure.KafkaConfig;
import com.dabom.messaging.kafka.autoconfigure.KafkaErrorHandlerConfig;
import com.dabom.messaging.kafka.error.KafkaExceptionClassifier;
import com.dabom.messaging.kafka.event.KafkaEventMessageSupport;
import com.dabom.messaging.kafka.event.publisher.DefaultKafkaEventPublisher;
import com.dabom.messaging.kafka.metrics.KafkaMetrics;
import com.dabom.messaging.kafka.metrics.consumer.KafkaMetricsRecordInterceptor;
import com.dabom.messaging.kafka.metrics.producer.KafkaMetricsProducerListener;
import com.dabom.messaging.kafka.support.KafkaEventMetadataExtractor;
import com.dabom.messaging.kafka.support.KafkaLogSanitizer;

@Configuration
@Import({
    KafkaConfig.class,
    KafkaErrorHandlerConfig.class,
    KafkaMetrics.class,
    KafkaExceptionClassifier.class,
    KafkaEventMetadataExtractor.class,
    KafkaLogSanitizer.class,
    KafkaEventMessageSupport.class,
    KafkaMetricsRecordInterceptor.class,
    KafkaMetricsProducerListener.class,
    DefaultKafkaEventPublisher.class
})
public class KafkaCommonConfig {}
