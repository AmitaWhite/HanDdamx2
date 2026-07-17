package com.white.handdam.category.service;

import com.white.handdam.category.dto.request.CreateCategoryRequest;
import com.white.handdam.category.dto.request.UpdateCategoryNameRequest;
import com.white.handdam.category.dto.response.CategoryResponse;
import com.white.handdam.category.entity.Category;
import com.white.handdam.category.exception.CategoryErrorCode;
import com.white.handdam.category.repository.CategoryRepository;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카테고리 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;

    /**
     * 활성 카테고리 목록 조회 (공개)
     */
    public List<CategoryResponse> getActiveCategories() {
        return categoryRepository.findAllByActiveTrueOrderByNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 전체 카테고리 목록 조회 (관리자)
     */
    public List<CategoryResponse> getAllCategories(Long adminId) {
        validateAdmin(adminId);
        return categoryRepository.findAllByOrderByNameAsc()
                .stream()
                .map(CategoryResponse::from)
                .toList();
    }

    /**
     * 카테고리 생성 (관리자)
     */
    @Transactional
    public CategoryResponse createCategory(Long adminId, CreateCategoryRequest request) {
        validateAdmin(adminId);
        if (categoryRepository.existsByName(request.name())) {
            throw new CustomException(CategoryErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        Category category = Category.builder()
                .name(request.name())
                .build();
        return CategoryResponse.from(categoryRepository.save(category));
    }

    /**
     * 카테고리명 수정 (관리자)
     */
    @Transactional
    public CategoryResponse updateCategoryName(Long adminId, Long categoryId, UpdateCategoryNameRequest request) {
        validateAdmin(adminId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        if (categoryRepository.existsByName(request.name())) {
            throw new CustomException(CategoryErrorCode.CATEGORY_NAME_DUPLICATE);
        }
        category.updateName(request.name());
        return CategoryResponse.from(category);
    }

    /**
     * 카테고리 활성·비활성 변경 (관리자)
     */
    @Transactional
    public CategoryResponse updateActivation(Long adminId, Long categoryId, boolean active) {
        validateAdmin(adminId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        category.setActive(active);
        return CategoryResponse.from(category);
    }

    /**
     * ADMIN role 검증 공통 메서드
     */
    private void validateAdmin(Long memberId) {
        memberRepository.findById(memberId)
                .filter(m -> m.getRole() == Role.ADMIN)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.ADMIN_ONLY));
    }
}