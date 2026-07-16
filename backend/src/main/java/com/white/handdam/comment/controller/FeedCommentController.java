package com.white.handdam.comment.controller;

import com.white.handdam.comment.dto.request.FeedCommentCreateRequest;
import com.white.handdam.comment.dto.response.FeedCommentIdResponse;
import com.white.handdam.comment.dto.response.FeedCommentResponse;
import com.white.handdam.comment.service.FeedCommentService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

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

    // [LYJ-016] POST /api/feeds/{feedId}/comments
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/feeds/{feedId}/comments")
    public ApiResponse<FeedCommentIdResponse> createComment(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody FeedCommentCreateRequest request
    ) {
        Long commentId = feedCommentService.createComment(feedId, member.id(), request);
        return ApiResponse.success(new FeedCommentIdResponse(commentId));
    }

    // [LYJ-017] POST /api/feeds/{feedId}/comments/{parentCommentId}/replies
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/feeds/{feedId}/comments/{parentCommentId}/replies")
    public ApiResponse<FeedCommentIdResponse> createReply(
            @PathVariable Long feedId,
            @PathVariable Long parentCommentId,
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody FeedCommentCreateRequest request
    ){
        Long commentId = feedCommentService.createReply(feedId, parentCommentId, member.id(), request);
        return ApiResponse.success(new FeedCommentIdResponse(commentId));
    }
}