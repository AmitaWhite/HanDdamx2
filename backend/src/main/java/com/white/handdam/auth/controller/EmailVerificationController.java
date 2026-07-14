package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.request.EmailVerificationConfirmRequest;
import com.white.handdam.auth.dto.request.EmailVerificationRequest;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth/email-verifications")
@RequiredArgsConstructor
public class EmailVerificationController {

    private final AuthService authService;

    // KSY-004 : 인증 메일 발송 (신규 인증 요청 생성)
    @PostMapping
    public ResponseEntity<ApiResponse<Void>> sendVerificationEmail(@Valid @RequestBody EmailVerificationRequest request) {
        authService.sendVerificationEmail(request.email());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

    // KSY-005
    @PostMapping("/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmVerificationEmail(@Valid @RequestBody EmailVerificationConfirmRequest request) {
        authService.confirmSignupVerification(request.token());
        return ResponseEntity.ok(ApiResponse.noContent());
    }

}
