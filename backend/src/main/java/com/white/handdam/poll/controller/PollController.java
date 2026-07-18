package com.white.handdam.poll.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.dto.response.PollResponse;
import com.white.handdam.poll.service.PollService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PollController {

    private final PollService pollService;

    // [LYJ-022] 피드에 투표 추가
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/feeds/{feedId}/poll")
    public ApiResponse<Long> createPoll(
            @PathVariable Long feedId,
            @AuthenticationPrincipal AuthMember member,
            @Valid @RequestBody PollCreateRequest request
    ) {
        return ApiResponse.success(pollService.createPoll(feedId, member.id(), request));
    }

    // [LYJ-023] 투표 선택지, 내 참여 조회
    @GetMapping("/api/polls/{pollId}")
    public ApiResponse<PollResponse> getPoll(
            @PathVariable Long pollId,
            @AuthenticationPrincipal AuthMember member
    ) {
        Long memberId = (member != null) ? member.id() : null;
        return ApiResponse.success(pollService.getPoll(pollId, memberId));
    }

}
