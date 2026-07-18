package com.white.handdam.poll.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;

public record PollCreateRequest(
        @NotBlank @Size(max = 500)
        String question,

        @NotNull
        Instant endAt,

        @NotNull
        @Size(min = 2, max = 10, message = "선택지는 2개 이상 10개 이하여야 합니다")
        List<@NotBlank @Size(max = 200) String> options
) {}
