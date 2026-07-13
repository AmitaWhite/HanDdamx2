package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.response.EmailAvailabilityResponse;
import com.white.handdam.auth.service.AuthService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
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

    // TODO: 공통 응답(ApiResponse) 및 GlobalExceptionHandler 적용 후 수정 필요
    @GetMapping("/email-availability")
    public EmailAvailabilityResponse checkEmailAvailability(@RequestParam @Email @NotBlank String email) {
        return new EmailAvailabilityResponse(authService.isEmailAvailable(email));
    }

}
