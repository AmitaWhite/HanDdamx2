package com.white.handdam.creator.dto.request;

/**
 * 크리에이터 전환 신청 요청 DTO.
 */
public record ApplyCreatorRequest(
        String introduction  // 크리에이터 소개글 (선택)
) {
}