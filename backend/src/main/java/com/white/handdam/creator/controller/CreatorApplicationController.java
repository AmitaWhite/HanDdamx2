package com.white.handdam.creator.controller;

import com.white.handdam.creator.dto.request.ApplyCreatorRequest;
import com.white.handdam.creator.dto.response.CreatorApplicationResponse;
import com.white.handdam.creator.entity.CreatorApplicationStatus;
import com.white.handdam.creator.service.CreatorApplicationService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 크리에이터 전환 신청 API
 *
 * 모든 응답 ApiResponse<T>로 래핑
 */
@RestController
@RequestMapping("/api/creator-applications")
@RequiredArgsConstructor
public class CreatorApplicationController {

    private final CreatorApplicationService creatorApplicationService;

    /**
     * 크리에이터 전환 신청
     * 권한: 일반 회원 (USER role)
     * 201 Created 반환
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<CreatorApplicationResponse> apply(
            @AuthenticationPrincipal AuthMember member,
            @RequestPart("metadata") ApplyCreatorRequest request,
            @RequestPart(value = "representativeImage", required = false) MultipartFile representativeImage
    ) {
        CreatorApplicationResponse response = creatorApplicationService.apply(
                member.id(), request, representativeImage);
        return ApiResponse.success(response);
    }

    /**
     * 내 최근 신청 상태 조회
     * 권한: 회원
     */
    @GetMapping("/me")
    public ApiResponse<CreatorApplicationResponse> getMyLatestApplication(
            @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(creatorApplicationService.getMyLatestApplication(member.id()));
    }

    /**
     * 내 신청 이력 전체 조회
     * 권한: 회원
     */
    @GetMapping("/me/history")
    public ApiResponse<List<CreatorApplicationResponse>> getMyApplicationHistory(
            @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(creatorApplicationService.getMyApplicationHistory(member.id()));
    }
}