package com.white.handdam.project.controller;

import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.project.dto.request.CreateProjectRequest;
import com.white.handdam.project.dto.request.UpdateProjectRequest;
import com.white.handdam.project.dto.response.ProjectResponse;
import com.white.handdam.project.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import jakarta.validation.Valid;

/**
 * 프로젝트 API 컨트롤러
 */
@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /**
     * 프로젝트 생성
     * 권한: 크리에이터
     */
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProjectResponse> createProject(
            @AuthenticationPrincipal AuthMember member,
            @RequestPart("metadata") @Valid CreateProjectRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {
        return ApiResponse.success(projectService.createProject(member.id(), request, coverImage));
    }

    /**
     * 프로젝트 상세 조회
     * 권한: 전체 (비로그인 포함)
     */
    @GetMapping("/{projectId}")
    public ApiResponse<ProjectResponse> getProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember member
    ) {
        Long requesterId = member != null ? member.id() : null;
        return ApiResponse.success(projectService.getProject(projectId, requesterId));
    }

    /**
     * 프로젝트 수정
     * 권한: 프로젝트 소유자
     */
    @PatchMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<ProjectResponse> updateProject(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long projectId,
            @RequestPart("metadata") @Valid UpdateProjectRequest request,
            @RequestPart(value = "coverImage", required = false) MultipartFile coverImage
    ) {
        return ApiResponse.success(
                projectService.updateProject(member.id(), projectId, request, coverImage));
    }

    /**
     * 프로젝트 소프트 삭제
     * 권한: 프로젝트 소유자
     */
    @DeleteMapping("/{projectId}")
    public ApiResponse<Void> deleteProject(
            @AuthenticationPrincipal AuthMember member,
            @PathVariable Long projectId
    ) {
        projectService.deleteProject(member.id(), projectId);
        return ApiResponse.success(null);
    }

    /**
     * 프로젝트 피드 목록 조회
     * 권한: 전체 (구독 등급에 따라 공개범위 적용)
     */
    @GetMapping("/{projectId}/feeds")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getProjectFeeds(
            @PathVariable Long projectId,
            @AuthenticationPrincipal AuthMember member,
            Pageable pageable
    ) {
        Long requesterId = member != null ? member.id() : null;
        return ApiResponse.success(SliceResponse.from(projectService.getProjectFeeds(projectId, requesterId, pageable)));
    }
}