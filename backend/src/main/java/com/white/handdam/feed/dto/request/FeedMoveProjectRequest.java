package com.white.handdam.feed.dto.request;

import jakarta.validation.constraints.NotNull;

public record FeedMoveProjectRequest(
        @NotNull Long projectId
) {}