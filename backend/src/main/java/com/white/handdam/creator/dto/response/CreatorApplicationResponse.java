package com.white.handdam.creator.dto.response;

import com.white.handdam.creator.entity.CreatorApplicationStatus;
import java.time.Instant;

/**
 * 크리에이터 전환 신청 응답 DTO
 */
public record CreatorApplicationResponse(
        Long id,
        Long memberId,
        CreatorApplicationStatus status,
        String introduction,
        String representativeImageUrl,
        Long reviewedBy,
        String rejectReason,
        Instant appliedAt,
        Instant reviewedAt
) {
}