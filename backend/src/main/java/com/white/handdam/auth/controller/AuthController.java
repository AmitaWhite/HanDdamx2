package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.request.EmailVerificationRequest;
import com.white.handdam.auth.dto.request.SignupRequest;
import com.white.handdam.auth.dto.response.EmailAvailabilityResponse;
import com.white.handdam.auth.dto.response.NicknameAvailabilityResponse;
import com.white.handdam.auth.dto.response.SignupResponse;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.auth.service.EmailVerificationService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Validated
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final EmailVerificationService emailVerificationService;

    // KSY-001
    @GetMapping("/email-availability")
    public ResponseEntity<ApiResponse<EmailAvailabilityResponse>> checkEmailAvailability(@RequestParam @Email @NotBlank String email) {
        EmailAvailabilityResponse response = new EmailAvailabilityResponse(
                authService.isEmailAvailable(email)
        );
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // KSY-002
    @GetMapping("/nickname-availability")
    public ResponseEntity<ApiResponse<NicknameAvailabilityResponse>> checkNicknameAvailability(@RequestParam @NotBlank String nickname) {
        NicknameAvailabilityResponse response = new NicknameAvailabilityResponse(
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

    // KSY-004 : 인증 메일 발송 (신규 인증 요청 생성)
    // - 이메일 변경, 비밀번호 재설정 최초 요청, 소셜 가입 후 이메일 검증 등...
    @PostMapping("/email-verifications")
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.sendVerificationEmail(request.email());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

}
