package com.white.handdam.member.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.dto.request.MemberUpdateRequest;
import com.white.handdam.member.dto.response.MemberProfileResponse;
import com.white.handdam.member.service.MemberService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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

    @PatchMapping(value = "/me/profile-image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<MemberProfileResponse> updateProfileImage(@AuthenticationPrincipal AuthMember authMember,
                                                                 @RequestPart("image") MultipartFile image) {
        return ApiResponse.success(memberService.updateProfileImage(authMember.id(), image));
    }

    @DeleteMapping(value = "/me/profile-image")
    public ApiResponse<MemberProfileResponse> deleteProfileImage(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(memberService.deleteProfileImage(authMember.id()));
    }

}
