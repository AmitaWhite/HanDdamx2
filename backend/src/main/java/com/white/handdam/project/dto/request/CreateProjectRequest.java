package com.white.handdam.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateProjectRequest(
        @NotBlank String title,
        String description,
        @NotNull Long categoryId
) {}