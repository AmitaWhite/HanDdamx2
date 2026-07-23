package com.white.handdam.comment.dto.response;

import java.time.Instant;
import java.util.List;

public record FeedCommentResponse(
        Long id,
        Long memberId,
        String nickname,
        String profileImageUrl,
        String content,
        int depth,
        Instant createdAt,
        Instant updatedAt,
        List<FeedCommentResponse> replies
) {}