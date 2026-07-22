package com.white.handdam.feed.service;

import com.white.handdam.category.entity.Category;
import com.white.handdam.category.exception.CategoryErrorCode;
import com.white.handdam.category.repository.CategoryRepository;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.feed.dto.request.AddAttachmentRequest;
import com.white.handdam.feed.dto.request.FeedCreateRequest;
import com.white.handdam.feed.dto.request.FeedMoveProjectRequest;
import com.white.handdam.feed.dto.request.FeedUpdateRequest;
import com.white.handdam.feed.dto.response.AttachmentResponse;
import com.white.handdam.feed.dto.response.DownloadResponse;
import com.white.handdam.feed.dto.response.FeedDetailResponse;
import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.feed.entity.AttachmentType;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.FeedAttachment;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.exception.FeedErrorCode;
import com.white.handdam.feed.repository.FeedAttachmentRepository;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.like.repository.FeedLikeRepository;
import com.white.handdam.poll.entity.Poll;
import com.white.handdam.poll.repository.PollRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.exception.ProjectErrorCode;
import com.white.handdam.project.repository.ProjectRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import com.white.handdam.subscription.entity.SubscriptionLevel;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedService {
    private final FeedRepository feedRepository;
    private final SubscriptionLevelChecker subscriptionLevelChecker;
    private final ProjectRepository projectRepository;
    private final FeedAttachmentRepository feedAttachmentRepository;
    private final ObjectStorage objectStorage;
    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final PollRepository pollRepository;
    private final FeedLikeRepository feedLikeRepository;

    // [LYJ-001] 피드 작성
    @Transactional
    public Long createFeed(Long memberId, FeedCreateRequest request) {
        Project project = projectRepository.findByIdAndDeletedFalse(request.projectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }
        Feed feed = Feed.create(request.projectId(), request.title(), request.content(), request.visibility());
        return feedRepository.save(feed).getId();
    }

    // [LYJ-002] 피드 상세 조회 + 공개범위 잠금 처리 [LYJ-030]
    public FeedDetailResponse getFeed(Long feedId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        // Member, Category 추가
        Member creator = memberRepository.findById(project.getCreatorId())
            .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        Category category = categoryRepository.findById(project.getCategoryId())
            .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));

        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = (memberId != null && !isOwner)
                ? subscriptionLevelChecker.getLevel(memberId, project.getCreatorId())
                : null;
        Long pollId = pollRepository.findByFeedId(feedId).map(Poll::getId).orElse(null);
        boolean liked = memberId != null && feedLikeRepository.existsByFeedIdAndMemberId(feedId, memberId);
        return canAccess(feed.getVisibility(), level, isOwner)
            ? FeedDetailResponse.visible(feed, creator, category, pollId, liked)
            : FeedDetailResponse.locked(feed, creator, category, pollId, liked);
    }

    // [LYJ-030] 피드 공개범위
    private boolean canAccess(Visibility visibility, String level, boolean isOwner) {
        if (isOwner) return true;
        return switch (visibility) {
            case PUBLIC          -> true;
            case FREE_SUBSCRIBER -> "FREE".equals(level) || "PAID".equals(level);
            case PAID_SUBSCRIBER -> "PAID".equals(level);
        };
    }

    // [LYJ-003] 피드 수정
    @Transactional
    public Long updateFeed(Long feedId, Long memberId, FeedUpdateRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }
        feed.update(request.title(), request.content(), request.visibility());
        return feed.getId();
    }

    // [LYJ-004] 피드 프로젝트 이동
    @Transactional
    public Long moveFeedProject(Long feedId, Long memberId, FeedMoveProjectRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project currentProject = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!currentProject.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        Project targetProject = projectRepository.findByIdAndDeletedFalse(request.projectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!targetProject.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }
        feed.moveProject(request.projectId());
        return feed.getId();
    }

    // [LYJ-005] 피드 소프트 삭제
    @Transactional
    public void deleteFeed(Long feedId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));

        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        feed.delete();
    }

    // [LYJ-006] 최근 PUBLIC 피드 목록
    public Slice<FeedSummaryResponse> getPublicFeeds(Pageable pageable) {
        return toSummarySlice(
            feedRepository.findByVisibilityAndDeletedFalse(Visibility.PUBLIC, pageable)
        );
    }

    // [LYJ-007] 회원 홈 피드 (구독 피드 + 카테고리 필터)
    public Slice<FeedSummaryResponse> getHomeFeed(Long memberId, Long categoryId, Pageable pageable) {
        Map<Long, SubscriptionLevel> levelMap = subscriptionLevelChecker.getActiveSubscriptionLevels(memberId);

        List<Long> paidCreatorIds = levelMap.entrySet().stream()
                .filter(e -> e.getValue() == SubscriptionLevel.PAID)
                .map(Map.Entry::getKey)
                .toList();

        List<Long> freeCreatorIds = levelMap.entrySet().stream()
                .filter(e -> e.getValue() == SubscriptionLevel.FREE)
                .map(Map.Entry::getKey)
                .toList();

        // IN() 빈 리스트 Hibernate 오류 방지
        List<Long> safePaid = paidCreatorIds.isEmpty() ? List.of(-1L) : paidCreatorIds;
        List<Long> safeFree = freeCreatorIds.isEmpty() ? List.of(-1L) : freeCreatorIds;

        return toSummarySlice(
            feedRepository.findHomeFeeds(
                memberId, safePaid, safeFree, categoryId,
                List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER),
                pageable
            )
        );
    }

    // [LYJ-008] 전체 공개 탐색 피드 (비회원도 접근 가능)
    public Slice<FeedSummaryResponse> getExploreFeeds(Long categoryId, Pageable pageable) {
        return toSummarySlice(
            feedRepository.findExploreFeeds(categoryId, pageable)
        );
    }

    // [LYJ-009] 특정 크리에이터의 피드 목록 조회
    public Slice<FeedSummaryResponse> getCreatorFeeds(Long creatorId, Long memberId, Pageable pageable) {
        String level = (memberId != null) ? subscriptionLevelChecker.getLevel(memberId, creatorId) : null;
        List<Visibility> visibilities = resolveVisibilites(level);

        return toSummarySlice(
            feedRepository.findByCreatorIdAndVisibilityIn(creatorId, visibilities, pageable)
        );
    }

    // [LYJ-010] 내 작성 피드 목록 (크리에이터 본인 전용 — 공개범위 무관 전체 조회)
    public Slice<FeedSummaryResponse> getMyFeeds(Long creatorId, Pageable pageable) {
        return toSummarySlice(
            feedRepository.findByCreatorId(creatorId, pageable)
        );
    }

    // 리스트 공통 변환 헬퍼 — N+1 방지 배치 조회
    private Slice<FeedSummaryResponse> toSummarySlice(Slice<Feed> feeds) {
        List<Long> projectIds = feeds.getContent().stream()
            .map(Feed::getProjectId).distinct().toList();
        Map<Long, Project> projectMap = projectRepository.findAllById(projectIds).stream()
            .collect(Collectors.toMap(Project::getId, p -> p));
        List<Long> creatorIds = projectMap.values().stream()
            .map(Project::getCreatorId).distinct().toList();
        List<Long> categoryIds = projectMap.values().stream()
            .map(Project::getCategoryId).distinct().toList();
        Map<Long, Member> memberMap = memberRepository.findAllById(creatorIds).stream()
            .collect(Collectors.toMap(Member::getId, m -> m));
        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
            .collect(Collectors.toMap(Category::getId, c -> c));
        return feeds.map(f -> {
            Project p = projectMap.get(f.getProjectId());
            return FeedSummaryResponse.from(
                f,
                memberMap.get(p.getCreatorId()),
                categoryMap.get(p.getCategoryId())
            );
        });
    }

    // 구독 레벨을 접근 가능한 공개범위 목록으로 변환
    private List<Visibility> resolveVisibilites(String level) {
        if ("PAID".equals(level)) {
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER, Visibility.PAID_SUBSCRIBER);
        } else if ("FREE".equals(level)) {
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER);
        }
        return List.of(Visibility.PUBLIC);
    }

    // [LYJ-012] 피드 첨부파일 추가
    // type=IMAGE/FILE: S3 업로드 후 저장 / type=VIDEO_LINK: 외부 URL만 저장
    @Transactional
    public AttachmentResponse addAttachment(Long feedId, Long memberId,
                                            MultipartFile file, AddAttachmentRequest request) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        FeedAttachment attachment;
        if (file != null && !file.isEmpty()) {
            AttachmentType type = resolveAttachmentType(file.getContentType());
            StoredObject stored = objectStorage.upload("feeds/" + feedId + "/attachments", file);
            attachment = FeedAttachment.ofUpload(feedId, type, stored, file.getSize(), file.getContentType());
        } else {
            if (request.videoUrl() == null || request.videoUrl().isBlank()) {
                throw new CustomException(FeedErrorCode.ATTACHMENT_INVALID_REQUEST);
            }
            attachment = FeedAttachment.ofVideoLink(feedId, request.videoUrl());
        }

        return AttachmentResponse.from(feedAttachmentRepository.save(attachment));
    }

    // MIME 타입으로 AttachmentType 결정
    private AttachmentType resolveAttachmentType(String mimeType) {
        if (mimeType == null) return AttachmentType.FILE;
        if (mimeType.startsWith("image/")) return AttachmentType.IMAGE;
        if (mimeType.startsWith("video/")) return AttachmentType.FILE;
        return AttachmentType.FILE; // PDF 등
    }

    // [LYJ-013] 피드 첨부파일 삭제
    @Transactional
    public void deleteAttachment(Long feedId, Long attachmentId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(FeedErrorCode.FEED_FORBIDDEN);
        }

        FeedAttachment attachment = feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(attachmentId, feedId)
            .orElseThrow(() -> new CustomException(FeedErrorCode.ATTACHMENT_NOT_FOUND));

        // S3 파일이 있는 경우만 삭제
        if (attachment.getStorageKey() != null) {
            objectStorage.delete(attachment.getStorageKey());
        }
        attachment.softDelete();
    }

    // [LYJ-014] 첨부파일 다운로드 URL 조회
    public DownloadResponse getDownloadUrl(Long feedId, Long attachmentId, Long memberId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
            .orElseThrow(() -> new CustomException(FeedErrorCode.FEED_NOT_FOUND));
        Project project = projectRepository.findByIdAndDeletedFalse(feed.getProjectId())
            .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));

        boolean isOwner = memberId != null && project.getCreatorId().equals(memberId);
        String level = (!isOwner && memberId != null)
            ? subscriptionLevelChecker.getLevel(memberId, project.getCreatorId())
            : null;
        validateAccess(feed.getVisibility(), level, isOwner);

        FeedAttachment attachment = feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(attachmentId, feedId)
            .orElseThrow(() -> new CustomException(FeedErrorCode.ATTACHMENT_NOT_FOUND));

        if (attachment.getType() == AttachmentType.VIDEO_LINK) {
            return DownloadResponse.ofLink(attachment);
        }
        String presignedUrl = objectStorage.generatePresignedUrl(attachment.getStorageKey(), 10);
        return DownloadResponse.ofPresigned(attachment, presignedUrl, 10);
    }

    // 공개범위 접근 불가 시 적절한 에러 코드로 예외 발생
    // canAccess()는 true/false만 반환하지만, 다운로드는 왜 막혔는지 명확히 알려줘야 함
    private void validateAccess(Visibility visibility, String level, boolean isOwner) {
        if (canAccess(visibility, level, isOwner)) return;
        throw new CustomException(
            visibility == Visibility.FREE_SUBSCRIBER
                ? FeedErrorCode.FREE_SUBSCRIPTION_REQUIRED
                : FeedErrorCode.PAID_SUBSCRIPTION_REQUIRED
        );
    }


}
