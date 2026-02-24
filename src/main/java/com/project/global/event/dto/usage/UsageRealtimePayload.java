package com.project.global.event.dto.usage;

public record UsageRealtimePayload(
        Long familyId,
        Long customerId,
        Long totalUsedBytes,
        Long totalLimitBytes,
        Long remainingBytes,
        Double usedPercent,
        Long monthlyUsedBytes,
        Double userUsagePercent,
        Long monthlyLimitBytes) {}
