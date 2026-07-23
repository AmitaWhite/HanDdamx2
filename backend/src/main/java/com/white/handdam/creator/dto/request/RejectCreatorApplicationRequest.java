package com.white.handdam.creator.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 크리에이터 신청 거절 요청 DTO.
 * PATCH .../reject 거절요청
 */
public record RejectCreatorApplicationRequest(
        @Size(max = 500) String rejectReason  // 거절 사유 (선택)
) {
}