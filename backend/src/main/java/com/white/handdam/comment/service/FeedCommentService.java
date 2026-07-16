package com.white.handdam.comment.service;

import com.white.handdam.comment.dto.response.FeedCommentResponse;
import com.white.handdam.comment.entity.FeedComment;
import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedCommentService {
    private final FeedRepository feedRepository;
    private final FeedCommentRepository feedCommentRepository;
    private final MemberRepository memberRepository;
    private final SubscriptionLevelChecker subscriptionLevelChecker;

    // [LYJ-015] 피드 댓글·대댓글 목록 조회
    public List<FeedCommentResponse> getComments(Long feedId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        // TODO [LYJ-015] Project 엔티티 추가 후 creatorId 꺼내서 level 확인
        boolean isOwner = false; // TODO: Project 추가 후 교체
        String level = null;     // TODO: Project 추가 후 subscriptionLevelChecker.getLevel(memberId, creatorId)
        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        List<FeedComment> all = feedCommentRepository
                .findByFeedIdAndDeletedFalseOrderByCreatedAtAsc(feedId);

        // 작성자 닉네임 한 번에 조회 (댓글마다 따로 조회하면 N+1 문제 발생)
        Set<Long> memberIds = all.stream()
                .map(c -> c.getMemberId())
                .collect(Collectors.toSet());

        Map<Long, String> nicknameMap = memberRepository.findAllById(memberIds).stream()
                .collect(Collectors.toMap(m -> m.getId(), m -> m.getNickname()));

        Map<Long, List<FeedComment>> repliesMap = all.stream()
                .filter(c -> c.getParentCommentId() != null)
                .collect(Collectors.groupingBy(c -> c.getParentCommentId()));

        return all.stream()
                .filter(c -> c.getParentCommentId() == null)
                .map(c -> toResponse(c, repliesMap, nicknameMap))
                .toList();
    }

    private FeedCommentResponse toResponse(
            FeedComment c,
            Map<Long, List<FeedComment>> repliesMap,
            Map<Long, String> nicknameMap
    )
    {
        // 이 댓글의 대댓글 꺼내기 (없으면 빈 리스트)
        List<FeedCommentResponse> replies = repliesMap
                .getOrDefault(c.getId(), List.of())
                .stream()
                .map(r -> toResponse(r, Map.of(), nicknameMap))
                .toList();

        return new FeedCommentResponse(
                c.getId(),
                c.getMemberId(),
                nicknameMap.getOrDefault(c.getMemberId(), "알 수 없음"),
                c.getContent(),
                c.getDepth(),
                c.getCreatedAt(),
                c.getUpdatedAt(),
                replies
        );
    }

    // [LYJ-030] FeedService.canAccess()와 동일 로직
    private boolean canAccess(Visibility visibility, String level, boolean isOwner) {
        if (isOwner) return true;
        return switch (visibility) {
            case PUBLIC          -> true;
            case FREE_SUBSCRIBER -> "FREE".equals(level) || "PAID".equals(level);
            case PAID_SUBSCRIBER -> "PAID".equals(level);
        };
    }
}