package com.white.handdam.poll.dto.request;

import java.time.Instant;

public record PollUpdateRequest(
    String question,
    Instant endAt
) {}
