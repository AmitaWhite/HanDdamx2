package com.white.handdam.creator.controller;

import com.white.handdam.creator.dto.response.CreatorProfileResponse;
import com.white.handdam.creator.dto.request.UpdateCreatorProfileRequest;
import com.white.handdam.creator.service.CreatorProfileService;
import com.white.handdam.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.multipart.MultipartFile;

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
     * 대표 이미지 변경
     */
    @PatchMapping(value = "/me/representative-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> updateRepresentativeImage(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        CreatorProfileResponse response = creatorProfileService.updateRepresentativeImage(memberId, image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 대표 이미지 제거
     */
    @DeleteMapping("/me/representative-image")
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> clearRepresentativeImage(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        CreatorProfileResponse response = creatorProfileService.clearRepresentativeImage(memberId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 커버 이미지 변경
     */
    @PatchMapping(value = "/me/cover-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> updateCoverImage(
            @RequestHeader("X-Member-Id") Long memberId,
            @RequestPart("image") MultipartFile image
    ) {
        CreatorProfileResponse response = creatorProfileService.updateCoverImage(memberId, image);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * 커버 이미지 제거
     */
    @DeleteMapping("/me/cover-image")
    public ResponseEntity<ApiResponse<CreatorProfileResponse>> clearCoverImage(
            @RequestHeader("X-Member-Id") Long memberId
    ) {
        CreatorProfileResponse response = creatorProfileService.clearCoverImage(memberId);
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