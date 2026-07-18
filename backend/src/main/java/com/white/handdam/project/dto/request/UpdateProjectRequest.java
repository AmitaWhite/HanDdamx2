package com.white.handdam.project.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record UpdateProjectRequest(
        @NotBlank String title,
        String description,
        @NotNull Long categoryId,
        boolean removeCoverImage
) {}