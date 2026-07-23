package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.request.EmailVerificationRequest;
import com.white.handdam.auth.dto.request.PasswordResetConfirmRequest;
import com.white.handdam.auth.dto.LoginResult;
import com.white.handdam.auth.dto.TokenRefreshResult;
import com.white.handdam.auth.dto.request.LoginRequest;
import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.AvailabilityResponse;
import com.white.handdam.auth.dto.response.LoginResponse;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.dto.response.TokenRefreshResponse;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.auth.service.PasswordService;
import com.white.handdam.auth.service.LoginService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.global.security.jwt.JwtProperties;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final LoginService loginService;
    private final JwtProperties jwtProperties;
    private final PasswordService passwordService;

    @Value("${app.cookie-secure}")
    private boolean cookieSecure;

    // KSY-001
    @GetMapping("/email-availability")
    public ApiResponse<AvailabilityResponse> checkEmailAvailability(@RequestParam @Email @NotBlank String email) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isEmailAvailable(email)
        );
        return ApiResponse.success(response);
    }

    // KSY-002
    @GetMapping("/nickname-availability")
    public ApiResponse<AvailabilityResponse> checkNicknameAvailability(@RequestParam @NotBlank String nickname) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isNicknameAvailable(nickname)
        );
        return ApiResponse.success(response);
    }

    // KSY-003
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ApiResponse.success(response);
    }

    // KSY-007
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        LoginResult result = loginService.login(request.email(), request.password());

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                jwtProperties.refreshExpiration() / 1000
        );
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        LoginResponse loginResponse = new LoginResponse(
                result.accessToken(), result.memberId(), result.nickname(), result.role()
        );

        return ApiResponse.success(loginResponse);
    }

    // Access Token 재발급
    @PostMapping("/token/refresh")
    public ApiResponse<TokenRefreshResponse> refresh(
            @CookieValue(value = "refreshToken", required = false) String refreshToken,
            HttpServletResponse response) {
        TokenRefreshResult result = loginService.refresh(refreshToken);

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                jwtProperties.refreshExpiration() / 1000
        );

        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

        return ApiResponse.success(new TokenRefreshResponse(result.accessToken()));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@AuthenticationPrincipal AuthMember authMember, HttpServletResponse response) {
        loginService.logout(authMember.id());

        response.addHeader(HttpHeaders.SET_COOKIE, createRefreshTokenCookie("", 0).toString()); // 브라우저 쿠키 삭제

        return ApiResponse.noContent();
    }

    private ResponseCookie createRefreshTokenCookie (String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // JS 접근 X
                .secure(cookieSecure) // local=false, 배포/prod=true
                .sameSite("Strict") // Cross Site 요청 X
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    // KSY-012
    @PostMapping("/password-reset")
    public ApiResponse<Void> requestPasswordReset(@Valid @RequestBody EmailVerificationRequest request) {
        passwordService.requestPasswordReset(request.email());
        return ApiResponse.noContent();
    }

    // KSY-013
    @PatchMapping("/password-reset")
    public ApiResponse<Void> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordService.resetPassword(request.token(), request.newPassword());
        return ApiResponse.noContent();
    }

}
