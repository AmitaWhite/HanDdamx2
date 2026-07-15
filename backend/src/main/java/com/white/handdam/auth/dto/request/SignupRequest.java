package com.white.handdam.auth.dto.request;

import com.white.handdam.auth.validation.ValidPassword;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record SignupRequest(
        @NotBlank(message = "이메일은 필수입니다.")
        @Email(message = "올바른 이메일 형식이 아닙니다.")
        String email,

        @NotBlank(message = "비밀번호는 필수입니다.")
        @ValidPassword
        String password,

        @NotBlank(message = "닉네임은 필수입니다.")
        @Pattern(regexp = "^[a-zA-Z0-9가-힣]{2,20}$", // TODO: 협의 필요
                 message = "닉네임은 2~20자의 한글, 영문, 숫자만 가능합니다.")
        String nickname
) {
}
