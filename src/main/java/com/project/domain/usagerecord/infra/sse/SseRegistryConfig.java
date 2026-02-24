package com.project.domain.usagerecord.infra.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseRegistryConfig {

    @Bean
    public SseEmitterRegistry memberUsageEmitterRegistryCore() {
        return new SseEmitterRegistry("MemberUsage");
    }

    @Bean
    public SseEmitterRegistry totalUsageEmitterRegistryCore() {
        return new SseEmitterRegistry("TotalUsage");
    }
}
