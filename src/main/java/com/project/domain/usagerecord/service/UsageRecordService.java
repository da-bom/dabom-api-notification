package com.project.domain.usagerecord.service;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import com.project.domain.family.repository.FamilyMemberRepository;
import com.project.domain.usagerecord.dto.response.RealtimeTotalUsageResponse;
import com.project.domain.usagerecord.dto.response.RealtimeUsageByMemberResponse;
import com.project.domain.usagerecord.infra.sse.MemberUsageSseEmitterRegistry;
import com.project.domain.usagerecord.infra.sse.TotalUsageSseEmitterRegistry;
import com.project.global.event.dto.usage.UsageRealtimePayload;
import com.project.global.exception.ApplicationException;
import com.project.global.exception.code.FamilyErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UsageRecordService {

    private final TotalUsageSseEmitterRegistry totalRegistry;
    private final MemberUsageSseEmitterRegistry memberRegistry;

    private final FamilyMemberRepository familyMemberRepository;

    public SseEmitter subscribeTotal(Long customerId) {
        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        return totalRegistry.register(familyId);
    }

    public SseEmitter subscribeByMember(Long customerId) {
        Long familyId =
                familyMemberRepository
                        .findFamilyIdByCustomerId(customerId)
                        .orElseThrow(
                                () -> new ApplicationException(FamilyErrorCode.FAMILY_NOT_FOUND));

        return memberRegistry.register(familyId);
    }

    public void pushTotalUsageBytes(UsageRealtimePayload payload) {
        RealtimeTotalUsageResponse response =
                new RealtimeTotalUsageResponse(
                        payload.familyId(),
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        totalRegistry.send(payload.familyId(), "usage-updated", response);
    }

    public void pushMemberUsageBytes(UsageRealtimePayload payload) {
        Long familyId = payload.familyId();
        Long customerId = payload.customerId();

        RealtimeUsageByMemberResponse response =
                new RealtimeUsageByMemberResponse(
                        familyId,
                        customerId,
                        payload.totalUsedBytes(),
                        payload.totalLimitBytes(),
                        payload.remainingBytes());

        memberRegistry.send(familyId, "usage-updated-by-member", response);
    }
}
