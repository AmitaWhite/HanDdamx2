package com.white.handdam.feed.dto.request;
import com.white.handdam.feed.entity.Visibility;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// [LYJ-001]
public record FeedCreateRequest(
        @NotNull Long projectId,
        @NotBlank @Size(max = 255) String title,
        @NotBlank String content,
        @NotNull Visibility visibility
) {}