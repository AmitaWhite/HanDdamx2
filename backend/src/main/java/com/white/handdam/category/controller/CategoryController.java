package com.white.handdam.category.controller;

import com.white.handdam.category.dto.response.CategoryResponse;
import com.white.handdam.category.service.CategoryService;
import com.white.handdam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리 공개 API
 */
@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /**
     * 활성 카테고리 목록 조회
     * 권한: 전체 (비로그인 포함)
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getActiveCategories() {
        return ApiResponse.success(categoryService.getActiveCategories());
    }
}