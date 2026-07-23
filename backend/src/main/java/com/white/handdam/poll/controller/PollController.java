package com.white.handdam.poll.controller;

import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import com.white.handdam.poll.dto.request.PollCreateRequest;
import com.white.handdam.poll.dto.request.PollUpdateRequest;
import com.white.handdam.poll.dto.request.PollVoteRequest;
import com.white.handdam.poll.dto.response.PollResponse;
import com.white.handdam.poll.dto.response.PollResultResponse;
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

    // [LYJ-024] 투표 질문·종료일 수정
    @PatchMapping("/api/polls/{pollId}")
    public ApiResponse<Long> updatePoll(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member,
        @Valid @RequestBody PollUpdateRequest request
    ) {
        return ApiResponse.success(pollService.updatePoll(pollId, member.id(), request));
    }

    // [LYJ-025] 투표 삭제
    @DeleteMapping("/api/polls/{pollId}")
    public ApiResponse<Void> deletePoll(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member
    ) {
        pollService.deletePoll(pollId, member.id());
        return ApiResponse.success(null);
    }

    // [LYJ-026] 투표 참여
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping("/api/polls/{pollId}/votes")
    public ApiResponse<Long> vote(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member,
        @Valid @RequestBody PollVoteRequest request
    ) {
        return ApiResponse.success(pollService.vote(pollId, member.id(), request));
    }

    // [LYJ-027] 내 투표 선택지 변경
    @PatchMapping("/api/polls/{pollId}/votes/me")
    public ApiResponse<Long> changeVote(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member,
        @Valid @RequestBody PollVoteRequest request
    ) {
        return ApiResponse.success(pollService.changeVote(pollId, member.id(), request));
    }

    // [LYJ-028] 투표 결과 조회
    @GetMapping("/api/polls/{pollId}/results")
    public ApiResponse<PollResultResponse> getPollResults(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member
    ) {
        Long memberId = (member != null) ? member.id() : null;
        return ApiResponse.success(pollService.getPollResults(pollId, memberId));
    }

    // [LYJ-029] 투표 조기 종료
    @PatchMapping("/api/polls/{pollId}/close")
    public ApiResponse<Long> closePoll(
        @PathVariable Long pollId,
        @AuthenticationPrincipal AuthMember member
    ) {
        return ApiResponse.success(pollService.closePoll(pollId, member.id()));
    }

}
