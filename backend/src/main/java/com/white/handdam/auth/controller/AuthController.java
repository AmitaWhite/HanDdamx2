package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.response.EmailAvailabilityResponse;
import com.white.handdam.auth.dto.response.NicknameAvailabilityResponse;
import com.white.handdam.auth.service.AuthService;
import com.white.handdam.global.response.ApiResponse;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

}
