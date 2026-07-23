package com.white.handdam.comment.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record FeedCommentUpdateRequest(@NotBlank @Size(max = 1000) String content) {}
