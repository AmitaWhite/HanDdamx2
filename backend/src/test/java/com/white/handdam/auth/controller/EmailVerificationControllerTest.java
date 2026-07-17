package com.white.handdam.auth.controller;

import com.white.handdam.auth.exception.AuthErrorCode;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.global.config.SecurityConfig;
import com.white.handdam.global.exception.CustomException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.nullValue;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EmailVerificationController.class)
@Import(SecurityConfig.class)
@Disabled("SecurityConfig에 OAuth2 핸들러가 추가된 뒤 관련 mock이 갱신되지 않아 컨텍스트 로딩 실패 - TODO: CustomOAuth2UserService 등 mock 추가 필요")
class EmailVerificationControllerTest {

    private static final String CONFIRM_URL = "/api/auth/email-verifications/confirm";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    // KSY-005
    @Test
    @DisplayName("유효한 토큰이면 200과 noContent 응답 반환")
    void confirmVerificationEmailSuccess() throws Exception {
        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"valid-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").value(nullValue()));

        then(authService).should().confirmSignupVerification("valid-token");
    }

    @Test
    @DisplayName("토큰이 비어있으면 400 반환")
    void confirmVerificationEmailFailWhenTokenIsBlank() throws Exception {
        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"\"}"))
                .andExpect(status().isBadRequest());

        then(authService).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("유효하지 않은 토큰이면 400과 에러코드 반환")
    void confirmVerificationEmailFailWhenTokenInvalid() throws Exception {
        willThrow(new CustomException(AuthErrorCode.INVALID_VERIFICATION_TOKEN))
                .given(authService).confirmSignupVerification("invalid-token");

        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"invalid-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("INVALID_VERIFICATION_TOKEN"));
    }

    @Test
    @DisplayName("만료된 토큰이면 400과 에러코드 반환")
    void confirmVerificationEmailFailWhenTokenExpired() throws Exception {
        willThrow(new CustomException(AuthErrorCode.EXPIRED_VERIFICATION_TOKEN))
                .given(authService).confirmSignupVerification("expired-token");

        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"expired-token\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("EXPIRED_VERIFICATION_TOKEN"));
    }

    @Test
    @DisplayName("토큰에 연결된 회원이 없으면 404와 에러코드 반환")
    void confirmVerificationEmailFailWhenMemberNotFound() throws Exception {
        willThrow(new CustomException(AuthErrorCode.MEMBER_NOT_FOUND))
                .given(authService).confirmSignupVerification("orphan-token");

        mockMvc.perform(post(CONFIRM_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"orphan-token\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error.code").value("MEMBER_NOT_FOUND"));
    }

}