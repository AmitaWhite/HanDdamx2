package com.white.handdam.creator.controller;

import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.project.dto.response.ProjectResponse;
import com.white.handdam.creator.dto.request.UpdateCreatorProfileRequest;
import com.white.handdam.creator.dto.request.UpdateSubscriptionPriceRequest;
import com.white.handdam.creator.service.CreatorProfileService;
import com.white.handdam.project.service.ProjectService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

/**
 * 크리에이터 프로필 API 컨트롤러
 */
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorProfileService creatorProfileService;
    private final ProjectService projectService;

    /**
     * 내 크리에이터 프로필 조회
     */
    @GetMapping("/me")
    public ApiResponse<CreatorProfileResponse> getMyProfile(
            @AuthenticationPrincipal AuthMember member
    ) {
        CreatorProfileResponse response = creatorProfileService.getMyProfile(member.id());
        return ApiResponse.success(response);
    }

    /**
     * 소개·구독 혜택 수정
     */
    @PatchMapping("/me")
    public ApiResponse<CreatorProfileResponse> updateProfile(
            @AuthenticationPrincipal AuthMember member,
            @RequestBody UpdateCreatorProfileRequest request
    ) {
        CreatorProfileResponse response = creatorProfileService.updateProfile(member.id(), request);
        return ApiResponse.success(response);
    }

    /**
     * 대표 이미지 변경
     */
    @PatchMapping(value = "/me/representative-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreatorProfileResponse> updateRepresentativeImage(
            @AuthenticationPrincipal AuthMember member,
            @RequestPart("image") MultipartFile image
    ) {
        CreatorProfileResponse response = creatorProfileService.updateRepresentativeImage(member.id(), image);
        return ApiResponse.success(response);
    }

    /**
     * 대표 이미지 제거
     */
    @DeleteMapping("/me/representative-image")
    public ApiResponse<CreatorProfileResponse> clearRepresentativeImage(
            @AuthenticationPrincipal AuthMember member
    ) {
        CreatorProfileResponse response = creatorProfileService.clearRepresentativeImage(member.id());
        return ApiResponse.success(response);
    }

    /**
     * 커버 이미지 변경
     */
    @PatchMapping(value = "/me/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreatorProfileResponse> updateCoverImage(
            @AuthenticationPrincipal AuthMember member,
            @RequestPart("image") MultipartFile image
    ) {
        CreatorProfileResponse response = creatorProfileService.updateCoverImage(member.id(), image);
        return ApiResponse.success(response);
    }

    /**
     * 커버 이미지 제거
     */
    @DeleteMapping("/me/cover-image")
    public ApiResponse<CreatorProfileResponse> clearCoverImage(
            @AuthenticationPrincipal AuthMember member
    ) {
        CreatorProfileResponse response = creatorProfileService.clearCoverImage(member.id());
        return ApiResponse.success(response);
    }

    /**
     * 특정 크리에이터 프로젝트 목록 조회
     * 권한: 전체 (비로그인 포함)
     */
    @GetMapping("/{creatorId}/projects")
    public ApiResponse<List<ProjectResponse>> getCreatorProjects(
            @PathVariable Long creatorId,
            @AuthenticationPrincipal AuthMember member
    ) {
        Long requesterId = member != null ? member.id() : null;
        return ApiResponse.success(projectService.getCreatorProjects(creatorId, requesterId));
    }

    /**
     * 내 프로젝트 관리 목록
     * 권한: 크리에이터
     */
    @GetMapping("/me/projects")
    public ApiResponse<List<ProjectResponse>> getMyProjects(
            @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(projectService.getCreatorProjects(member.id(), member.id()));
    }

    /**
     * 월 구독 가격 변경
     */
    @PatchMapping("/me/subscription-price")
    public ApiResponse<CreatorProfileResponse> updateSubscriptionPrice(
            @AuthenticationPrincipal AuthMember member,
            @RequestBody UpdateSubscriptionPriceRequest request
    ) {
        CreatorProfileResponse response = creatorProfileService.updateSubscriptionPrice(member.id(), request);
        return ApiResponse.success(response);
    }

    /**
     * 크리에이터 공개 프로필 조회
     */
    @GetMapping("/{creatorId}")
    public ApiResponse<CreatorProfileResponse> getPublicProfile(
            @PathVariable Long creatorId,
            @AuthenticationPrincipal AuthMember member
    ) {
        CreatorProfileResponse response = creatorProfileService.getPublicProfile(
                creatorId, member != null ? member.id() : null);
        return ApiResponse.success(response);
    }
}