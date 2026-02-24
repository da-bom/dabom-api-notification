package com.project.domain.usagerecord.dto.response;

public record RealtimeUsageByMemberResponse(
        Long familyId, Long customerId, Long monthlyUsedBytes) {}
