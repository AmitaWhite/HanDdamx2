package com.white.handdam.member.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.response.SliceResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.member.dto.request.MemberUpdateRequest;
import com.white.handdam.member.dto.response.*;
import com.white.handdam.member.service.MemberService;
import com.white.handdam.member.service.MyActivityService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final MyActivityService myActivityService;

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

    @GetMapping("/{memberId}")
    public ApiResponse<MemberPublicProfileResponse> getPublicProfile (@PathVariable Long memberId) {
        return ApiResponse.success(memberService.getPublicProfile(memberId));
    }

    @GetMapping("/me/feed-comments")
    public ApiResponse<SliceResponse<MyFeedCommentResponse>> getMyFeedComments(@AuthenticationPrincipal AuthMember authMember,
                                                                       @PageableDefault(size=3) Pageable pageable) {
        return ApiResponse.success(SliceResponse.from(myActivityService.getMyFeedComments(authMember.id(), pageable)));
    }

    @GetMapping("/me/board-comments")
    public ApiResponse<SliceResponse<MyBoardCommentResponse>> getMyBoardComments(@AuthenticationPrincipal AuthMember authMember,
                                                                         @PageableDefault(size=3) Pageable pageable) {
        return ApiResponse.success(SliceResponse.from(myActivityService.getMyBoardComments(authMember.id(), pageable)));
    }

    @GetMapping("/me/summary")
    public ApiResponse<MemberSummaryResponse> getMySummary(@AuthenticationPrincipal AuthMember authMember) {
        return ApiResponse.success(myActivityService.getMySummary(authMember.id()));
    }

}
