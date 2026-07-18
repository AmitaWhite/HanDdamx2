package com.white.handdam.member.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MemberUpdateRequest(
    @NotBlank(message = "닉네임은 필수입니다.")
    @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,20}$",
        message = "닉네임은 2~20자의 한글, 영문, 숫자만 가능합니다.")
    String nickname
) {
}
