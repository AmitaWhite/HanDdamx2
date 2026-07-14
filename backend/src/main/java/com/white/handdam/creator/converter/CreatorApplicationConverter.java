package com.white.handdam.creator.converter;

import com.white.handdam.creator.dto.response.CreatorApplicationResponse;
import com.white.handdam.creator.entity.CreatorApplication;

public final class CreatorApplicationConverter {

    private CreatorApplicationConverter() {
    }

    /** 프로필 정보 없이 변환 (사용자용) */
    public static CreatorApplicationResponse toResponse(CreatorApplication application) {
        return toResponse(application, null, null);
    }

    /** 프로필 정보 포함 변환 (관리자용) */
    public static CreatorApplicationResponse toResponse(CreatorApplication application,
                                                        String introduction,
                                                        String representativeImageUrl) {
        return new CreatorApplicationResponse(
                application.getId(),
                application.getMemberId(),
                application.getStatus(),
                introduction,
                representativeImageUrl,
                application.getReviewedBy(),
                application.getRejectReason(),
                application.getAppliedAt(),
                application.getReviewedAt()
        );
    }
}