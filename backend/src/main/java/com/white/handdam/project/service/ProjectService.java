package com.white.handdam.project.service;

import com.white.handdam.category.entity.Category;
import com.white.handdam.category.exception.CategoryErrorCode;
import com.white.handdam.category.repository.CategoryRepository;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.feed.entity.AttachmentType;
import com.white.handdam.feed.entity.Feed;
import com.white.handdam.feed.entity.FeedAttachment;
import com.white.handdam.feed.repository.FeedAttachmentRepository;
import com.white.handdam.feed.repository.FeedRepository;
import com.white.handdam.like.entity.FeedLike;
import com.white.handdam.like.repository.FeedLikeRepository;
import com.white.handdam.global.exception.CommonErrorCode;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.member.entity.Member;
import com.white.handdam.member.entity.Role;
import com.white.handdam.member.repository.MemberRepository;
import com.white.handdam.project.converter.ProjectConverter;
import com.white.handdam.project.dto.request.CreateProjectRequest;
import com.white.handdam.project.dto.request.UpdateProjectRequest;
import com.white.handdam.project.dto.response.ProjectResponse;
import com.white.handdam.project.entity.Project;
import com.white.handdam.project.exception.ProjectErrorCode;
import com.white.handdam.project.repository.ProjectRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import com.white.handdam.feed.dto.response.FeedSummaryResponse;
import com.white.handdam.feed.entity.Visibility;
import com.white.handdam.feed.service.SubscriptionLevelChecker;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 프로젝트 비즈니스 로직
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProjectService {

    private final ProjectRepository projectRepository;
    private final CategoryRepository categoryRepository;
    private final MemberRepository memberRepository;
    private final FeedRepository feedRepository;
    private final ObjectStorage objectStorage;
    private final SubscriptionLevelChecker subscriptionLevelChecker;
    private final FeedLikeRepository feedLikeRepository;
    private final FeedAttachmentRepository feedAttachmentRepository;


    /**
     * 프로젝트 생성
     */
    @Transactional
    public ProjectResponse createProject(Long memberId, CreateProjectRequest request,
                                         MultipartFile coverImage) {
        Member creator = findCreatorMemberById(memberId);
        Category category = findActiveCategory(request.categoryId());

        String coverImageUrl = null;
        String coverImageStorageKey = null;
        if (coverImage != null && !coverImage.isEmpty()) {
            StoredObject stored = objectStorage.upload("projects/" + memberId, coverImage);
            coverImageUrl = stored.url();
            coverImageStorageKey = stored.storageKey();
        }

        Project project = Project.builder()
                .creatorId(memberId)
                .categoryId(request.categoryId())
                .title(request.title())
                .description(request.description())
                .coverImageUrl(coverImageUrl)
                .coverImageStorageKey(coverImageStorageKey)
                .build();

        Project saved = projectRepository.save(project);
        return ProjectConverter.toResponse(saved, creator, category, 0L, true);
    }

    /**
     * 프로젝트 상세 조회
     */
    public ProjectResponse getProject(Long projectId, Long requesterId) {
        Project project = findProjectById(projectId);
        Member creator = memberRepository.findById(project.getCreatorId())
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        Category category = categoryRepository.findById(project.getCategoryId())
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        long feedCount = feedRepository.countByProjectIdAndDeletedFalse(projectId);
        boolean isMine = requesterId != null && requesterId.equals(project.getCreatorId());
        return ProjectConverter.toResponse(project, creator, category, feedCount, isMine);
    }

    /**
     * 특정 크리에이터의 프로젝트 목록 조회
     */
    public List<ProjectResponse> getCreatorProjects(Long creatorMemberId, Long requesterId) {
        Member creator = findCreatorMemberById(creatorMemberId);
        List<Project> projects = projectRepository
                .findByCreatorIdAndDeletedFalseOrderByCreatedAtDesc(creatorMemberId);
        if (projects.isEmpty()) {
            return List.of();
        }

        // 카테고리 배치 조회
        Set<Long> categoryIds = projects.stream().map(Project::getCategoryId).collect(Collectors.toSet());
        Map<Long, Category> categoryMap = categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, c -> c));

        // 피드 수 배치 조회 (또는 `@Query로` GROUP BY)
        List<Long> projectIds = projects.stream().map(Project::getId).toList();
        // FeedRepository에 countByProjectIdInAndDeletedFalseGroupByProjectId 추가 필요
        Map<Long, Long> feedCountMap = feedRepository.countByProjectIdInAndDeletedFalse(projectIds)
            .stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));

        return projects.stream()
                .map(p -> {
                    Category category = Optional.ofNullable(categoryMap.get(p.getCategoryId()))
                            .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
                    long feedCount = feedCountMap.getOrDefault(p.getId(), 0L);
                    boolean isMine = requesterId != null && requesterId.equals(creatorMemberId);
                    return ProjectConverter.toResponse(p, creator, category, feedCount, isMine);
                })
                .toList();
    }

    /**
     * 프로젝트 수정
     */
    @Transactional
    public ProjectResponse updateProject(Long memberId, Long projectId,
                                         UpdateProjectRequest request, MultipartFile coverImage) {
        Project project = findProjectById(projectId);
        validateOwner(project, memberId);
        Category category = findActiveCategory(request.categoryId());

        project.update(request.title(), request.description(), request.categoryId());

        if (request.removeCoverImage()) {
            // 기존 S3 객체 삭제
            if (project.getCoverImageStorageKey() != null) {
                objectStorage.delete(project.getCoverImageStorageKey());
            }
            project.clearCoverImage();
        } else if (coverImage != null && !coverImage.isEmpty()) {
            // 기존 S3 객체 삭제 후 새 파일 업로드
            if (project.getCoverImageStorageKey() != null) {
                objectStorage.delete(project.getCoverImageStorageKey());
            }
            StoredObject stored = objectStorage.upload("projects/" + memberId, coverImage);
            project.updateCoverImage(stored.url(), stored.storageKey());
        }

        Member creator = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        long feedCount = feedRepository.countByProjectIdAndDeletedFalse(projectId);
        return ProjectConverter.toResponse(project, creator, category, feedCount, true);
    }

    /**
     * 프로젝트 소프트 삭제.
     * 활성 피드가 존재하면 PROJECT_NOT_EMPTY 에러
     */
    @Transactional
    public void deleteProject(Long memberId, Long projectId) {
        Project project = findProjectById(projectId);
        validateOwner(project, memberId);

        long feedCount = feedRepository.countByProjectIdAndDeletedFalse(projectId);
        if (feedCount > 0) {
            throw new CustomException(ProjectErrorCode.PROJECT_NOT_EMPTY);
        }
        project.delete();
    }

    /**
     * CREATOR role 회원 조회 공통 메서드
     */
    private Member findCreatorMemberById(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        if (member.getRole() != Role.CREATOR) {
            throw new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND);
        }
        return member;
    }

    /**
     * 활성 카테고리 조회
     */
    private Category findActiveCategory(Long categoryId) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new CustomException(CommonErrorCode.INVALID_REQUEST);
        }
        return category;
    }

    /**
     * 삭제되지 않은 프로젝트 조회
     */
    private Project findProjectById(Long projectId) {
        return projectRepository.findByIdAndDeletedFalse(projectId)
                .orElseThrow(() -> new CustomException(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    /**
     * 프로젝트 피드 목록 조회
     */
    // getProjectFeeds 메서드 교체
    public Slice<FeedSummaryResponse> getProjectFeeds(Long projectId, Long requesterId, Pageable pageable) {
        Project project = findProjectById(projectId);
        Member creator = memberRepository.findById(project.getCreatorId())
            .orElseThrow(() -> new CustomException(CreatorErrorCode.CREATOR_NOT_FOUND));
        Category category = categoryRepository.findById(project.getCategoryId())
            .orElseThrow(() -> new CustomException(CategoryErrorCode.CATEGORY_NOT_FOUND));
        boolean isOwner = requesterId != null && requesterId.equals(project.getCreatorId());
        Slice<Feed> feeds = isOwner
            ? feedRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId, pageable)
            : feedRepository.findByProjectIdAndVisibilityInAndDeletedFalseOrderByCreatedAtDesc(
            projectId, resolveVisibilities(requesterId, project.getCreatorId()), pageable);

        List<Long> feedIds = feeds.getContent().stream().map(Feed::getId).toList();
        Set<Long> likedFeedIds = requesterId == null
            ? Set.of()
            : feedLikeRepository.findByMemberIdAndFeedIdIn(requesterId, feedIds).stream()
                .map(FeedLike::getFeedId)
                .collect(Collectors.toSet());
        Map<Long, FeedAttachment> thumbnailByFeedId = thumbnailsByFeedId(feedIds);

        return feeds.map(f -> {
            FeedAttachment thumbnail = thumbnailByFeedId.get(f.getId());
            return FeedSummaryResponse.from(
                f, creator, category, likedFeedIds.contains(f.getId()),
                thumbnail == null ? null : thumbnail.getUrl(),
                thumbnail == null ? null : thumbnailType(thumbnail)
            );
        });
    }

    // 피드별 대표 썸네일(첫 이미지, 없으면 첫 업로드 동영상) 배치 조회 — N+1 방지
    private Map<Long, FeedAttachment> thumbnailsByFeedId(List<Long> feedIds) {
        if (feedIds.isEmpty()) return Map.of();
        List<FeedAttachment> attachments =
            feedAttachmentRepository.findByFeedIdInAndDeletedFalseOrderByOrderIndex(feedIds);
        Map<Long, FeedAttachment> thumbnailByFeedId = new HashMap<>();
        for (FeedAttachment a : attachments) {
            if (!isThumbnailCandidate(a)) continue;
            thumbnailByFeedId.putIfAbsent(a.getFeedId(), a);
        }
        return thumbnailByFeedId;
    }

    // 대표 썸네일 후보: 업로드 이미지, 또는 업로드 동영상 파일 (외부 VIDEO_LINK는 미리보기를 만들 수 없어 제외)
    private static boolean isThumbnailCandidate(FeedAttachment a) {
        if (a.getType() == AttachmentType.IMAGE) return true;
        return a.getType() == AttachmentType.FILE
            && a.getMimeType() != null
            && a.getMimeType().startsWith("video/");
    }

    private static String thumbnailType(FeedAttachment a) {
        return a.getType() == AttachmentType.IMAGE ? "IMAGE" : "VIDEO";
    }

    // CANCEL_SCHEDULED 구독 만료 여부 체크 후 활성여부 판단
    private List<Visibility> resolveVisibilities(Long memberId, Long creatorId) {
        if (memberId == null) return List.of(Visibility.PUBLIC);

        String level = subscriptionLevelChecker.getLevel(memberId, creatorId);
        if ("PAID".equals(level)) {
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER, Visibility.PAID_SUBSCRIBER);
        }
        if ("FREE".equals(level)) {
            return List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER);
        }
        return List.of(Visibility.PUBLIC);
    }

    /**
     * 소유자 검증
     */
    private void validateOwner(Project project, Long memberId) {
        if (!project.getCreatorId().equals(memberId)) {
            throw new CustomException(ProjectErrorCode.PROJECT_FORBIDDEN);
        }
    }
}
