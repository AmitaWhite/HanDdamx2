package com.white.handdam.category.controller;

import com.white.handdam.category.dto.request.CreateCategoryRequest;
import com.white.handdam.category.dto.request.UpdateCategoryNameRequest;
import com.white.handdam.category.dto.response.CategoryResponse;
import com.white.handdam.category.service.CategoryService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 카테고리 관리자 API
 */
@RestController
@RequestMapping("/api/admin/categories")
@RequiredArgsConstructor
public class AdminCategoryController {

    private final CategoryService categoryService;

    /**
     * 전체 카테고리 목록 조회
     */
    @GetMapping
    public ApiResponse<List<CategoryResponse>> getAllCategories(
            @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(categoryService.getAllCategories(member.id()));
    }

    /**
     * 카테고리 생성
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<CategoryResponse> createCategory(
            @AuthenticationPrincipal AuthMember member,
            @RequestBody CreateCategoryRequest request
    ) {
        return ApiResponse.success(categoryService.createCategory(member.id(), request));
    }

    /**
     * 카테고리명 수정
     */
    @PatchMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategoryName(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long categoryId,
            @RequestBody UpdateCategoryNameRequest request
    ) {
        return ApiResponse.success(categoryService.updateCategoryName(member.id(), categoryId, request));
    }

    /**
     * 활성·비활성 변경
     */
    @PatchMapping("/{categoryId}/activation")
    public ApiResponse<CategoryResponse> updateActivation(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long categoryId,
            @RequestParam boolean active
    ) {
        return ApiResponse.success(categoryService.updateActivation(member.id(), categoryId, active));
    }
}