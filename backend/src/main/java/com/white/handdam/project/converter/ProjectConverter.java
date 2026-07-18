package com.white.handdam.project.converter;

import com.white.handdam.category.entity.Category;
import com.white.handdam.member.entity.Member;
import com.white.handdam.project.dto.response.ProjectResponse;
import com.white.handdam.project.entity.Project;

/**
 * Project 엔티티 → 응답 DTO 변환
 */
public final class ProjectConverter {

    private ProjectConverter() {}

    public static ProjectResponse toResponse(Project project, Member creator,
                                             Category category, long feedCount, boolean isMine) {
        return new ProjectResponse(
                project.getId(),
                new ProjectResponse.CreatorSummary(
                        creator.getId(),
                        creator.getNickname(),
                        creator.getProfileImageUrl()
                ),
                new ProjectResponse.CategorySummary(
                        category.getId(),
                        category.getName()
                ),
                project.getTitle(),
                project.getDescription(),
                project.getCoverImageUrl(),
                feedCount,
                isMine,
                project.getCreatedAt(),
                project.getUpdatedAt()
        );
    }
}