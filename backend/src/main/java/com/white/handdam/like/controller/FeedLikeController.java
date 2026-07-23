package com.white.handdam.like.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.like.dto.response.FeedLikeResponse;
import com.white.handdam.like.service.FeedLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class FeedLikeController {
    private final FeedLikeService feedLikeService;

    // [LYJ-020] POST /api/feeds/{feedId}/likes
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/feeds/{feedId}/likes")
    public ApiResponse<FeedLikeResponse> addLike(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member
    ){
        return ApiResponse.success(feedLikeService.addLike(feedId, member.id()));
    }

    // [LYJ-021] DELETE /api/feeds/{feedId}/likes
    @DeleteMapping("/api/feeds/{feedId}/likes")
    public ApiResponse<FeedLikeResponse> cancelLike(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member
    ){
        return ApiResponse.success(feedLikeService.cancelLike(feedId, member.id()));
    }
}
