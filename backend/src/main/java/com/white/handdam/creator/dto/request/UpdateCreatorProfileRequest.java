package com.white.handdam.creator.dto.request;

/**
 * 크리에이터 프로필 수정 요청 DTO
 */
public record UpdateCreatorProfileRequest(
        String introduction,
        String benefitsDescription
) {}