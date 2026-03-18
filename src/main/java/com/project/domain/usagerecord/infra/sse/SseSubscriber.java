package com.project.domain.usagerecord.infra.sse;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.common.exception.ApplicationException;
import com.project.common.exception.code.FamilyErrorCode;
import com.project.domain.family.repository.FamilyMemberRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class SseSubscriber {

    private final FamilyMemberRepository familyMemberRepository;
    private final EmitterRegistry registry;

    // 1) customerId로 familyId를 조회합니다.
    // 2) familyId 기준 SSE emitter를 등록해 구독을 시작합니다.
    public SseEmitter subscribe(Long customerId) {
        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        return registry.register(familyId);
    }
}
