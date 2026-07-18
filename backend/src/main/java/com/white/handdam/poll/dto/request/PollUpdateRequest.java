package com.white.handdam.poll.dto.request;

import com.white.handdam.poll.validation.AtLeastOneNotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

@AtLeastOneNotNull(fields = {"question", "endAt"}, message = "question 또는 endAt 중 하나는 필수입니다")
public record PollUpdateRequest(
    @Size(min = 1, max = 500)
    String question,
    Instant endAt
) {}
