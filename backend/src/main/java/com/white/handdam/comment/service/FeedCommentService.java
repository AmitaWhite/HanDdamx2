package com.white.handdam.comment.service;

import com.white.handdam.comment.dto.request.FeedCommentCreateRequest;
import com.white.handdam.comment.dto.request.FeedCommentUpdateRequest;
import com.white.handdam.comment.dto.response.FeedCommentResponse;
import com.white.handdam.comment.entity.FeedComment;
import com.white.handdam.comment.event.FeedCommentCreatedEvent;
import com.white.handdam.comment.event.FeedReplyCreatedEvent;
import com.white.handdam.comment.exception.FeedCommentErrorCode;
import com.white.handdam.comment.repository.FeedCommentRepository;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.exception.ProjectErrorCode;
import com.white.handdam.project.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
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
    private final ProjectRepository projectRepository;
    private final SubscriptionLevelChecker subscriptionLevelChecker;
    private final ApplicationEventPublisher eventPublisher;

    // [LYJ-015] 피드 댓글·대댓글 목록 조회
    public List<FeedCommentResponse> getComments(Long feedId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = isOwner ? null : subscriptionLevelChecker.getLevel(memberId, project.getCreatorId());
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

    // [LYJ-016] 댓글 작성
    @Transactional
    public Long createComment(Long feedId, Long memberId, FeedCommentCreateRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = isOwner ? null : subscriptionLevelChecker.getLevel(memberId, project.getCreatorId());

        if (!canAccess(feed.getVisibility(), level, isOwner)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        FeedComment comment = FeedComment.create(feedId, memberId, null, (short) 0, request.content());
        FeedComment saved = feedCommentRepository.save(comment);
        eventPublisher.publishEvent(new FeedCommentCreatedEvent(
            feedId,
            saved.getId(),
            memberId,               // 댓글 작성자
            project.getCreatorId(),
            request.content()
        ));
        return saved.getId();

    }

    // [LYJ-017] 대댓글 작성
    @Transactional
    public Long createReply(Long feedId, Long parentCommentId, Long memberId, FeedCommentCreateRequest request){
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = isOwner ? null : subscriptionLevelChecker.getLevel(memberId, project.getCreatorId());

        if(!canAccess(feed.getVisibility(), level, isOwner)){
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        FeedComment parent = feedCommentRepository.findByIdAndDeletedFalse(parentCommentId)
                .orElseThrow(() -> new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND));

        if(!parent.getFeedId().equals(feedId)){
            throw new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND);
        }

        if(parent.getDepth() != 0){
            throw new CustomException(FeedCommentErrorCode.COMMENT_CANNOT_REPLY);
        }

        FeedComment reply = FeedComment.create(feedId, memberId, parentCommentId, (short) 1, request.content());
        FeedComment saved = feedCommentRepository.save(reply);
        eventPublisher.publishEvent(new FeedReplyCreatedEvent(
            feedId,
            parentCommentId,
            saved.getId(),
            memberId,              // 대댓글 작성자
            parent.getMemberId(),  // 부모 댓글 작성자
            request.content()
        ));
        return saved.getId();

    }

    // [LYJ-018] 댓글 수정
    @Transactional
    public Long updateComment(Long feedId, Long commentId, Long memberId, FeedCommentUpdateRequest request){
        feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        FeedComment comment = feedCommentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND));

        if(!comment.getFeedId().equals(feedId)){
            throw new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND);
        }

        if(!comment.getMemberId().equals(memberId)){
            throw new CustomException(FeedCommentErrorCode.COMMENT_FORBIDDEN);
        }

        comment.updateContent(request.content());
        return comment.getId();
    }

    // [LYJ-019] 댓글 삭제
    @Transactional
    public void deleteComment(Long feedId, Long commentId, Long memberId){
        feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        FeedComment comment = feedCommentRepository.findByIdAndDeletedFalse(commentId)
                .orElseThrow(() -> new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getFeedId().equals(feedId)){
            throw new CustomException(FeedCommentErrorCode.COMMENT_NOT_FOUND);
        }

        if(!comment.getMemberId().equals(memberId)){
            throw new CustomException(FeedCommentErrorCode.COMMENT_FORBIDDEN);
        }

        comment.delete();
        if(comment.getDepth() == 0){
            feedCommentRepository.findByParentCommentIdAndDeletedFalse(commentId)
                    .forEach(reply -> reply.delete());
        }
    }

    // [LYJ-030]
    private boolean canAccess(Visibility visibility, String level, boolean isOwner) {
        if (isOwner) return true;
        return switch (visibility) {
            case PUBLIC          -> true;
            case FREE_SUBSCRIBER -> "FREE".equals(level) || "PAID".equals(level);
            case PAID_SUBSCRIBER -> "PAID".equals(level);
        };
    }
}
