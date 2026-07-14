package com.white.handdam.creator.controller;

import com.white.handdam.creator.dto.request.RejectCreatorApplicationRequest;
import com.white.handdam.creator.dto.response.CreatorApplicationResponse;
import com.white.handdam.creator.entity.CreatorApplicationStatus;
import com.white.handdam.creator.service.CreatorApplicationService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 크리에이터 신청 관리자 API.
 *
 * 권한 검증은 서비스 레이어에서 member.role == ADMIN 확인
 * (Spring Security 미적용 — SecurityConfig.java permitAll 상태)
 */
@RestController
@RequestMapping("/api/admin/creator-applications")
@RequiredArgsConstructor
public class AdminCreatorApplicationController {

    private final CreatorApplicationService creatorApplicationService;

    /**
     * 신청 목록 조회
     * status 파라미터로 PENDING/APPROVED/REJECTED 필터 가능, 생략 시 전체
     */
    @GetMapping
    public ApiResponse<Page<CreatorApplicationResponse>> getApplicationList(
            @RequestHeader("X-Member-Id") Long adminId,
            @RequestParam(required = false) CreatorApplicationStatus status,
            Pageable pageable
    ) {
        return ApiResponse.success(
                creatorApplicationService.getApplicationList(adminId, status, pageable));
    }

    /**
     * 신청 상세 조회
     */
    @GetMapping("/{applicationId}")
    public ApiResponse<CreatorApplicationResponse> getApplicationDetail(
            @RequestHeader("X-Member-Id") Long adminId,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(
                creatorApplicationService.getApplicationDetail(adminId, applicationId));
    }

    /**
     * 신청 승인
     * 승인 시: role → CREATOR, CREATOR_PROFILE 생성, 상태 → APPROVED
     */
    @PatchMapping("/{applicationId}/approve")
    public ApiResponse<CreatorApplicationResponse> approve(
            @RequestHeader("X-Member-Id") Long adminId,
            @PathVariable Long applicationId
    ) {
        return ApiResponse.success(creatorApplicationService.approve(adminId, applicationId));
    }

    /**
     * 신청 거절
     * rejectReason 필수 (최대 500자)
     */
    @PatchMapping("/{applicationId}/reject")
    public ApiResponse<CreatorApplicationResponse> reject(
            @RequestHeader("X-Member-Id") Long adminId,
            @PathVariable Long applicationId,
            @Valid @RequestBody RejectCreatorApplicationRequest request
    ) {
        return ApiResponse.success(
                creatorApplicationService.reject(adminId, applicationId, request));
    }
}