package com.white.handdam.auth.controller;

import com.white.handdam.auth.dto.request.PasswordChangeRequest;
import com.white.handdam.auth.service.PasswordService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/members/me/password")
@RequiredArgsConstructor
public class MemberPasswordController {

    private final PasswordService passwordService;

    @PatchMapping
    public ApiResponse<Void> changePassword(@AuthenticationPrincipal AuthMember member,
                                            @Valid @RequestBody PasswordChangeRequest request) {
        passwordService.changePassword(member.id(), request.currentPassword(), request.newPassword());
        return ApiResponse.noContent();
    }

}
