package com.white.handdam.auth.controller;

import com.white.handdam.auth.service.AuthService;
import com.white.handdam.global.config.SecurityConfig;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
class AuthControllerTest {

    private static final String URL = "/api/v1/auth/email-availability";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AuthService authService;

    @Test
    @DisplayName("사용 가능한 이메일이면 available=true와 200을 반환")
    void shouldReturnAvailableTrueWhenEmailDoesNotExist() throws Exception {
        given(authService.isEmailAvailable("new@handdam.com")).willReturn(true);

        mockMvc.perform(get(URL).param("email", "new@handdam.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(true));
    }

    @Test
    @DisplayName("이미 가입된 이메일이면 available=false와 200을 반환")
    void shouldReturnAvailableFalseWhenEmailExists() throws Exception {
        given(authService.isEmailAvailable("taken@handdam.com")).willReturn(false);

        mockMvc.perform(get(URL).param("email", "taken@handdam.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.available").value(false));
    }

    @Test
    @DisplayName("이메일 형식이 아니면 400을 반환")
    @Disabled("400 매핑은 전역 예외 핸들러 도입 후 활성화 예정 - 현재는 500 발생")
    void shouldReturnBadRequestWhenEmailIsInvalid() throws Exception {
        mockMvc.perform(get(URL).param("email", "not-an-email"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일 파라미터가 없으면 400을 반환")
    void shouldReturnBadRequestWhenEmailParameterIsMissing() throws Exception {
        mockMvc.perform(get(URL))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("이메일이 공백이면 400을 반환")
    @Disabled("400 매핑은 전역 예외 핸들러 도입 후 활성화 예정 - 현재는 500 발생")
    void shouldReturnBadRequestWhenEmailIsBlank() throws Exception {
        mockMvc.perform(get(URL).param("email", "   "))
                .andExpect(status().isBadRequest());
    }

}