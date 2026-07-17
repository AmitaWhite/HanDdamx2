package com.white.handdam.feed.controller;

import com.white.handdam.feed.dto.request.FeedCreateRequest;
import com.white.handdam.feed.dto.request.FeedMoveProjectRequest;
import com.white.handdam.feed.dto.request.FeedUpdateRequest;
import com.white.handdam.feed.dto.response.FeedDetailResponse;
import com.white.handdam.feed.dto.response.FeedIdResponse;
import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.feed.service.FeedService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // [LYJ-001] POST /api/feeds
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public ApiResponse<FeedIdResponse> createFeed(
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody FeedCreateRequest request
    ) {
        Long feedId = feedService.createFeed(member.id(), request);
        return ApiResponse.success(new FeedIdResponse(feedId));
    }

    // [LYJ-002] GET /api/feeds/{feedId}
    @GetMapping("/{feedId}")
    public ApiResponse<FeedDetailResponse> getFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(feedService.getFeed(feedId, member != null ? member.id() : null));
    }

    // [LYJ-003] PATCH /api/feeds/{feedId}
    @PatchMapping("/{feedId}")
    public ApiResponse<FeedIdResponse> updateFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody FeedUpdateRequest request
    ) {
        return ApiResponse.success(new FeedIdResponse(feedService.updateFeed(feedId, member.id(), request)));
    }

    // [LYJ-004] PATCH /api/feeds/{feedId}/project
    @PatchMapping("/{feedId}/project")
    public ApiResponse<FeedIdResponse> moveFeedProject(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody FeedMoveProjectRequest request
    ){
        return ApiResponse.success(new FeedIdResponse(feedService.moveFeedProject(feedId, member.id(), request)));
    }

    // [LYJ-005] DELETE /api/feeds/{feedId}
    @DeleteMapping("/{feedId}")
    public ApiResponse<Void> deleteFeed(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member
    ){
        feedService.deleteFeed(feedId, member.id());
        return ApiResponse.noContent();
    }

    // [LYJ-006] GET /api/feeds/public
    @GetMapping("/public")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getPublicFeeds(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ApiResponse.success(SliceResponse.from(feedService.getPublicFeeds(pageable)));
    }

    // [LYJ-007] GET /api/feeds/home
    @GetMapping("/home")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getHomeFeed(
            @AuthenticationPrincipal AuthMember member,
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ApiResponse.success(SliceResponse.from(feedService.getHomeFeed(member.id(), categoryId, pageable)));
    }

    // [LYJ-008] GET /api/feeds/explore
    @GetMapping("/explore")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getExploreFeeds(
            @RequestParam(required = false) Long categoryId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        return ApiResponse.success(SliceResponse.from(feedService.getExploreFeeds(categoryId, pageable)));
    }

    // [LYJ-009] GET /api/feeds/creators/{creatorId}
    @GetMapping("/creators/{creatorId}")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getCreatorFeeds(
            @PathVariable Long creatorId,
            @AuthenticationPrincipal AuthMember member,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ){
        Long memberId = (member != null) ? member.id() : null;
        return ApiResponse.success(SliceResponse.from(feedService.getCreatorFeeds(creatorId, memberId, pageable)));
    }

    // [LYJ-010] GET /api/feeds/me — 내 작성 피드 목록
    // TODO [LYJ-010] CREATOR 역할만 접근 가능하도록 추후 @PreAuthorize("hasRole('CREATOR')") 추가
    @GetMapping("/me")
    public ApiResponse<SliceResponse<FeedSummaryResponse>> getMyFeeds(
            @AuthenticationPrincipal AuthMember member,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return ApiResponse.success(SliceResponse.from(feedService.getMyFeeds(member.id(), pageable)));
    }
}
