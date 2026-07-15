package com.white.handdam.creator.controller;

import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.creator.service.CreatorProfileService;
import com.white.handdam.creator.dto.request.UpdateCreatorProfileRequest;
import com.white.handdam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 크리에이터 프로필 API 컨트롤러
 */
@RestController
@RequestMapping("/api/creators")
@RequiredArgsConstructor
public class CreatorController {

    private final CreatorProfileService creatorProfileService;

    /**
     * 내 크리에이터 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> getMyProfile(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        CreatorProfileResponse response = creatorProfileService.getMyProfile(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 소개·구독 혜택 수정
     */
    @PatchMapping("/me")
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> updateProfile(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestBody UpdateCreatorProfileRequest request
    ) {
        CreatorProfileResponse response = creatorProfileService.updateProfile(memberId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 크리에이터 공개 프로필 조회
     */
    @GetMapping("/{creatorId}")
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> getPublicProfile(
            @PathVariable Long creatorId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId
    ) {
        CreatorProfileResponse response = creatorProfileService.getPublicProfile(creatorId, memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}