package com.white.handdam.comment.controller;

import com.white.handdam.comment.dto.response.FeedCommentResponse;
import com.white.handdam.comment.service.FeedCommentService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FeedCommentController {

    private final FeedCommentService feedCommentService;

    // [LYJ-015] GET /api/feeds/{feedId}/comments
    @GetMapping("/api/feeds/{feedId}/comments")
    public ApiResponse<List<FeedCommentResponse>> getComments(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member
    ) {
        Long memberId = (member != null) ? member.id() : null;
        return ApiResponse.success(feedCommentService.getComments(feedId, memberId));
    }
}