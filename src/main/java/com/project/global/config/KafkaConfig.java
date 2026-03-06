package com.project.global.config;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import lombok.RequiredArgsConstructor;

/**
 * Kafka 설정
 *
 * <p>키는 String으로 변환 값은 JSON으로 변환 에러처리는 일단 ErrorHandlingDeserializer 맡김
 */
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    public static final String BROADCAST_KAFKA_LISTENER_CONTAINER_FACTORY =
            "broadcastKafkaListenerContainerFactory";

    private static final int GROUP_ID_SUFFIX_LENGTH = 8;

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    @Value("${spring.kafka.consumer.group-id}")
    private String defaultGroupId;

    @Value("${spring.kafka.broadcast.group-id-prefix}")
    private String broadcastGroupIdPrefix;

    // ========================================================================
    // 1. Producer 설정
    // ========================================================================
    @Bean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> config = new HashMap<>();
        config.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG,
                StringSerializer.class); // 키는 String으로 변환
        config.put(
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class); // 값은 JSON으로 변환

        // Type 정보 헤더를 넣지 않음 (Consumer에서 DTO 매핑 시 혼란 방지)
        config.put(JsonSerializer.ADD_TYPE_INFO_HEADERS, false);
        return new DefaultKafkaProducerFactory<>(config);
    }

    @Bean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    // ========================================================================
    // 2. Consumer 공통 설정
    // ========================================================================
    private Map<String, Object> consumerBaseConfig() {
        Map<String, Object> config = new HashMap<>();
        config.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        config.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        config.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
        config.put(ErrorHandlingDeserializer.VALUE_DESERIALIZER_CLASS, StringDeserializer.class);
        config.put(JsonDeserializer.TRUSTED_PACKAGES, "com.project.*");
        return config;
    }

    // ========================================================================
    // 3. 기본 Consumer 설정 (공유 group ID → 로드밸런싱)
    // ========================================================================
    @Bean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> config = consumerBaseConfig();
        config.put(ConsumerConfig.GROUP_ID_CONFIG, defaultGroupId);
        return new DefaultKafkaConsumerFactory<>(config);
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        return factory;
    }

    // ========================================================================
    // 4. Broadcast Consumer 설정 (인스턴스마다 고유 group ID → 브로드캐스팅)
    // ========================================================================
    @Bean(BROADCAST_KAFKA_LISTENER_CONTAINER_FACTORY)
    public ConcurrentKafkaListenerContainerFactory<String, Object>
            broadcastKafkaListenerContainerFactory() {
        Map<String, Object> config = consumerBaseConfig();
        config.put(
                ConsumerConfig.GROUP_ID_CONFIG,
                broadcastGroupIdPrefix
                        + "-"
                        + UUID.randomUUID().toString().substring(0, GROUP_ID_SUFFIX_LENGTH));

        ConcurrentKafkaListenerContainerFactory<String, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(new DefaultKafkaConsumerFactory<>(config));
        return factory;
    }
}
