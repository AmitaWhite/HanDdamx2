package com.white.handdam.category.dto.response;

import com.white.handdam.category.entity.Category;

/**
 * 카테고리 응답 DTO
 */
public record CategoryResponse(Long categoryId, String name, boolean active) {

    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getName(), category.isActive());
    }
}