package com.white.handdam.member.service;

import com.white.handdam.board.repository.BoardCommentRepository;
import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.creator.service.CreatorProfileService;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.dto.response.MemberSummaryResponse;
import com.white.handdam.member.dto.response.MyBoardCommentResponse;
import com.white.handdam.member.dto.response.MyFeedCommentResponse;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.exception.MemberErrorCode;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyActivityService {

    private final MemberRepository memberRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final BoardCommentRepository boardCommentRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final CreatorProfileService creatorProfileService;

    // KSY-020
    public Slice<MyFeedCommentResponse> getMyFeedComments(Long memberId, Pageable pageable) {
        return feedCommentRepository.findMyComments(memberId, pageable);
    }

    // KSY-021
    public Slice<MyBoardCommentResponse> getMyBoardComments(Long memberId, Pageable pageable) {
        return boardCommentRepository.findMyComments(memberId, pageable);
    }

    // KSY-015
    public MemberSummaryResponse getMySummary(Long memberId) {
        Member member = memberRepository.findById(memberId)
            .orElseThrow(() -> new CustomException(MemberErrorCode.MEMBER_NOT_FOUND));

        long boardCommentCount = boardCommentRepository.countMyComments(memberId);
        long subscribingCount = subscriptionRepository.countBySubscriberId(memberId);

        String introduction = member.getRole() == Role.CREATOR
            ? creatorProfileService.getIntroductionByMemberId(memberId)
            : null;

        return new MemberSummaryResponse(
            member.getId(),
            member.getNickname(),
            member.getProfileImageUrl(),
            member.getRole(),
            introduction,
            boardCommentCount,
            subscribingCount
        );
    }

}
