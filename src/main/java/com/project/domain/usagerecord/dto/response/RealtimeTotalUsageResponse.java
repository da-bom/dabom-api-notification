package com.project.domain.usagerecord.dto.response;

public record RealtimeTotalUsageResponse(
        Long familyId, Long totalUsedBytes, Long totalQuotaBytes, Long remainingBytes) {}
