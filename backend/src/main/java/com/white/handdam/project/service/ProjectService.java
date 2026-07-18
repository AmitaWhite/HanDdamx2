package com.white.handdam.project.service;

import com.white.handdam.category.entity.Category;
import com.white.handdam.category.exception.CategoryErrorCode;
import com.white.handdam.category.repository.CategoryRepository;
import com.white.handdam.creator.exception.CreatorErrorCode;
import com.white.handdam.feed.repository.FeedRepository;
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
import com.white.handdam.subscription.entity.SubscriptionLevel;
import com.white.handdam.subscription.entity.SubscriptionStatus;
import com.white.handdam.subscription.repository.SubscriptionRepository;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.List;

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
    private final SubscriptionRepository subscriptionRepository;


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
        Map<Long, Long> feedCountMap = ...;

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
    public Slice<FeedSummaryResponse> getProjectFeeds(Long projectId, Long requesterId, Pageable pageable) {
        Project project = findProjectById(projectId);

        boolean isOwner = requesterId != null && requesterId.equals(project.getCreatorId());
        if (isOwner) {
            return feedRepository.findByProjectIdAndDeletedFalseOrderByCreatedAtDesc(projectId, pageable)
                    .map(FeedSummaryResponse::from);
        }

        List<Visibility> visibilities = resolveVisibilities(requesterId, project.getCreatorId());
        return feedRepository.findByProjectIdAndVisibilityInAndDeletedFalseOrderByCreatedAtDesc(
                        projectId, visibilities, pageable)
                .map(FeedSummaryResponse::from);
    }

    private List<Visibility> resolveVisibilities(Long memberId, Long creatorId) {
        if (memberId == null) return List.of(Visibility.PUBLIC);

        return subscriptionRepository
                .findBySubscriberIdAndCreatorId(memberId, creatorId)
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE
                        || s.getStatus() == SubscriptionStatus.CANCEL_SCHEDULED)
                .map(s -> s.getSubscriptionLevel() == SubscriptionLevel.PAID
                        ? List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER, Visibility.PAID_SUBSCRIBER)
                        : List.of(Visibility.PUBLIC, Visibility.FREE_SUBSCRIBER))
                .orElse(List.of(Visibility.PUBLIC));
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