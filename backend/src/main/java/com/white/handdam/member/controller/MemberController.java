package com.white.handdam.member.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.dto.request.MemberUpdateRequest;
import com.white.handdam.member.dto.response.MemberProfileResponse;
import com.white.handdam.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;

    @GetMapping("/me")
    public ApiResponse<MemberProfileResponse> getMyProfile(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(memberService.getMyProfile(authMember.id()));
    }

    @PatchMapping("/me")
    public ApiResponse<MemberProfileResponse> updateMyProfile(@AuthenticationPrincipal AuthMember authMember,
                                                              @Valid @RequestBody MemberUpdateRequest request) {
        return ApiResponse.success(memberService.updateMyProfile(authMember.id(), request));
    }

}
