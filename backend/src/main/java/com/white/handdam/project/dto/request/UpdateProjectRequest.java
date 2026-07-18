package com.white.handdam.project.dto.request;

/**
 * 프로젝트 수정 요청 DTO
 */
public record UpdateProjectRequest(
        String title,
        String description,
        Long categoryId,
        boolean removeCoverImage
) {}