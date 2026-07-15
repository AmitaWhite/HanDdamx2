package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.request.EmailVerificationRequest;
import com.white.handdam.auth.dto.request.PasswordResetConfirmRequest;
import com.white.handdam.auth.dto.LoginResult;
import com.white.handdam.auth.dto.request.LoginRequest;
import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.AvailabilityResponse;
import com.white.handdam.auth.dto.response.LoginResponse;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.auth.service.PasswordResetService;
import com.white.handdam.auth.service.LoginService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.jwt.JwtProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
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
    private final PasswordResetService passwordResetService;

    // KSY-001
    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkEmailAvailability(@RequestParam @Email @NotBlank String email) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isEmailAvailable(email)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // KSY-002
    @GetMapping("/nickname-availability")
    public ResponseEntity<ApiResponse<AvailabilityResponse>> checkNicknameAvailability(@RequestParam @NotBlank String nickname) {
        AvailabilityResponse response = new AvailabilityResponse(
                authService.isNicknameAvailable(nickname)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // KSY-003
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<SignupResponse>> signup(@Valid @RequestBody SignupRequest request) {
        SignupResponse response = authService.signup(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success(response));
    }

    // KSY-007
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
        LoginResult result = loginService.login(request.email(), request.password());

        ResponseCookie cookie = createRefreshTokenCookie(
                result.refreshToken(),
                jwtProperties.refreshExpiration() / 1000
        );

        LoginResponse response = new LoginResponse(
                result.accessToken(), result.memberId(), result.nickname(), result.role()
        );

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(ApiResponse.success(response));

    }

    private ResponseCookie createRefreshTokenCookie (String refreshToken, long maxAgeSeconds) {
        return ResponseCookie.from("refreshToken", refreshToken)
                .httpOnly(true) // JS 접근 X
                .secure(false) // TODO: 배포 시 true 변경 (https)
                .sameSite("Strict") // Cross Site 요청 X
                .path("/")
                .maxAge(maxAgeSeconds)
                .build();
    }

    // KSY-012
    @PostMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> requestPasswordReset(@Valid @RequestBody EmailVerificationRequest request) {
        passwordResetService.requestPasswordReset(request.email());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // KSY-013
    @PatchMapping("/password-reset")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

}
