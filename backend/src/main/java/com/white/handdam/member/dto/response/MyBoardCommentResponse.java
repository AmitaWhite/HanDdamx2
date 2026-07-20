package com.white.handdam.member.dto.response;

import java.time.Instant;

public record MyBoardCommentResponse(
    Long commentId,
    String content,
    Long boardPostId,
    String boardPostTitle,
    Instant createdAt
) {
}
