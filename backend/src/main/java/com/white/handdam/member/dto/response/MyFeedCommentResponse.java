package com.white.handdam.member.dto.response;

import java.time.Instant;

public record MyFeedCommentResponse(
    Long commentId,
    String content,
    Long feedId,
    String feedTitle,
    Instant createdAt
) {
}
