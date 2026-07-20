package com.white.handdam.poll.dto.request;

import jakarta.validation.constraints.NotNull;

public record PollVoteRequest(@NotNull Long optionId) {}
