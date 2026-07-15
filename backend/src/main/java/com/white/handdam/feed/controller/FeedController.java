package com.white.handdam.feed.controller;

import com.white.handdam.feed.dto.request.FeedCreateRequest;
import com.white.handdam.feed.dto.request.FeedMoveProjectRequest;
import com.white.handdam.feed.dto.request.FeedUpdateRequest;
import com.white.handdam.feed.dto.response.FeedDetailResponse;
import com.white.handdam.feed.dto.response.FeedIdResponse;
import com.white.handdam.feed.service.FeedService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/feeds")
@RequiredArgsConstructor
public class FeedController {

    private final FeedService feedService;

    // [LYJ-001] POST /api/feeds
    @PostMapping
    public ResponseEntity<ApiResponse<FeedIdResponse>> createFeed(
            // TODO JWT랑 연결 필요 현재 임시로 X-Member-Id 사용
//            @AuthenticationPrincipal MemberDto dto,
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody FeedCreateRequest request
            )
    {
        Long feedId = feedService.createFeed(memberId, request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(new FeedIdResponse(feedId)));
    }

    // [LYJ-002] GET /api/feeds/{feedId}
    @GetMapping("/{feedId}")
    public ApiResponse<FeedDetailResponse> getFeed(
            @PathVariable Long feedId,
            @RequestHeader(value = "X-Member-Id", required = false) Long memberId
    ) {
        return ApiResponse.success(feedService.getFeed(feedId, memberId));
    }

    // [LYJ-003] PATCH /api/feeds/{feedId}
    @PatchMapping("/{feedId}")
    public ApiResponse<FeedIdResponse> updateFeed(
            @PathVariable Long feedId,
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody FeedUpdateRequest request
    ) {
        return ApiResponse.success(new FeedIdResponse(feedService.updateFeed(feedId, memberId, request)));
    }

    // [LYJ-004] PATCH /api/feeds/{feedId}/project
    @PatchMapping("/{feedId}/project")
    public ApiResponse<FeedIdResponse> moveFeedProject(
            @PathVariable Long feedId,
            @RequestHeader("X-Member-Id") Long memberId,
            @Valid @RequestBody FeedMoveProjectRequest request
    ){
        return ApiResponse.success(new FeedIdResponse(feedService.moveFeedProject(feedId, memberId, request)));
    }

    // [LYJ-005] DELETE /api/feeds/{feedId}
    @DeleteMapping("/{feedId}")
    public ApiResponse<Void> deleteFeed(
            @PathVariable Long feedId,
            @RequestHeader("X-Member-Id") Long memberId
    ){
        feedService.deleteFeed(feedId, memberId);
        return ApiResponse.noContent();
    }
}
