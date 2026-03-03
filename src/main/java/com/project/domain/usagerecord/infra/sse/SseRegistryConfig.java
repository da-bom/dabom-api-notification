package com.project.domain.usagerecord.infra.sse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SseRegistryConfig {

    // 멤버 사용량 SSE용 코어 레지스트리 빈을 생성합니다.
    @Bean
    public SseEmitterRegistry memberUsageEmitterRegistryCore() {
        return new SseEmitterRegistry("MemberUsage");
    }

    // 전체 사용량 SSE용 코어 레지스트리 빈을 생성합니다.
    @Bean
    public SseEmitterRegistry totalUsageEmitterRegistryCore() {
        return new SseEmitterRegistry("TotalUsage");
    }
}
