package com.white.handdam.feed.service;

import com.white.handdam.category.entity.Category;
import com.white.handdam.category.repository.CategoryRepository;
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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class FeedServiceTest {

    @Mock private FeedRepository feedRepository;
    @Mock private SubscriptionLevelChecker subscriptionLevelChecker;
    @Mock private ProjectRepository projectRepository;
    @Mock private MemberRepository memberRepository;
    @Mock private CategoryRepository categoryRepository;
    @Mock private FeedAttachmentRepository feedAttachmentRepository;
    @Mock private ObjectStorage objectStorage;
    @Mock private PollRepository pollRepository;
    @Mock private FeedLikeRepository feedLikeRepository;
    @InjectMocks private FeedService feedService;

    // ---------------------------------------------------------------
    // LYJ-001 피드 작성
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-001] 크리에이터가 자신의 프로젝트에 피드를 작성하면 feedId를 반환한다")
    void createFeed_success() {
        FeedCreateRequest request = new FeedCreateRequest(1L, "제목", "내용", Visibility.PUBLIC);
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));
        Feed saved = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.save(any(Feed.class))).willReturn(saved);

        Long result = feedService.createFeed(1L, request);

        assertThat(result).isEqualTo(1L);
        verify(feedRepository).save(any(Feed.class));
    }

    @Test
    @DisplayName("[LYJ-001] 다른 크리에이터의 프로젝트에 피드 작성 시 FEED_FORBIDDEN 예외 발생")
    void createFeed_notOwner_forbidden() {
        FeedCreateRequest request = new FeedCreateRequest(1L, "제목", "내용", Visibility.PUBLIC);
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(99L))); // creatorId=99L ≠ memberId=1L

        assertThatThrownBy(() -> feedService.createFeed(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
        verify(feedRepository, never()).save(any());
    }

    @Test
    @DisplayName("[LYJ-001] 존재하지 않는 프로젝트에 피드 작성 시 PROJECT_NOT_FOUND 예외 발생")
    void createFeed_projectNotFound() {
        FeedCreateRequest request = new FeedCreateRequest(999L, "제목", "내용", Visibility.PUBLIC);
        given(projectRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.createFeed(1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(ProjectErrorCode.PROJECT_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-002 + LYJ-030 피드 상세 조회 + 공개범위 잠금
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-002] PUBLIC 피드는 누구나 내용을 볼 수 있다")
    void getFeed_public_visibleToAll() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(memberRepository.findById(99L)).willReturn(Optional.of(sampleMember(99L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));

        FeedDetailResponse result = feedService.getFeed(1L, null);

        assertThat(result.locked()).isFalse();
        assertThat(result.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("[LYJ-002] PUBLIC 피드는 비로그인도 볼 수 있다")
    void getFeed_public_noLogin() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(memberRepository.findById(99L)).willReturn(Optional.of(sampleMember(99L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));
        given(pollRepository.findByFeedId(1L)).willReturn(Optional.of(samplePoll(1L, 7L)));

        FeedDetailResponse result = feedService.getFeed(1L, null);

        assertThat(result.locked()).isFalse();
        // pollId 조회는 memberId와 무관하므로 비로그인이어도 투표가 있으면 채워진다. liked만 memberId=null이라 기본값 false.
        assertThat(result.pollId()).isEqualTo(7L);
        assertThat(result.liked()).isFalse();
    }

    @Test
    @DisplayName("[LYJ-002] 투표가 있고 좋아요를 누른 상태면 pollId·liked가 올바르게 채워진다")
    void getFeed_withPollAndLiked_setsFieldsCorrectly() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(memberRepository.findById(99L)).willReturn(Optional.of(sampleMember(99L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));
        given(pollRepository.findByFeedId(1L)).willReturn(Optional.of(samplePoll(1L, 7L)));
        given(feedLikeRepository.existsByFeedIdAndMemberId(1L, 6L)).willReturn(true);

        FeedDetailResponse result = feedService.getFeed(1L, 6L);

        assertThat(result.pollId()).isEqualTo(7L);
        assertThat(result.liked()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-002] 크리에이터 본인은 PAID_SUBSCRIBER 피드도 볼 수 있다")
    void getFeed_owner_canSeePaidFeed() {
        Feed feed = sampleFeed(Visibility.PAID_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(1L))); // creatorId=memberId=1L
        given(memberRepository.findById(1L)).willReturn(Optional.of(sampleMember(1L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));

        FeedDetailResponse result = feedService.getFeed(1L, 1L);

        assertThat(result.locked()).isFalse();
        assertThat(result.content()).isEqualTo("내용");
    }

    @Test
    @DisplayName("[LYJ-030] FREE_SUBSCRIBER 피드는 비구독자에게 잠긴다")
    void getFeed_freeSubscriber_lockedForNonSubscriber() {
        Feed feed = sampleFeed(Visibility.FREE_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(6L, 99L)).willReturn(null); // 비구독자
        given(memberRepository.findById(99L)).willReturn(Optional.of(sampleMember(99L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));
        given(pollRepository.findByFeedId(1L)).willReturn(Optional.of(samplePoll(1L, 7L)));
        given(feedLikeRepository.existsByFeedIdAndMemberId(1L, 6L)).willReturn(true);

        FeedDetailResponse result = feedService.getFeed(1L, 6L);

        assertThat(result.locked()).isTrue();
        assertThat(result.content()).isNull();
        assertThat(result.requiredLevel()).isEqualTo("FREE_SUBSCRIBER");
        // 잠긴 응답이어도 pollId·liked는 본문과 무관하게 그대로 채워진다
        assertThat(result.pollId()).isEqualTo(7L);
        assertThat(result.liked()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-030] PAID_SUBSCRIBER 피드는 비구독자에게 잠긴다")
    void getFeed_paidSubscriber_lockedForNonSubscriber() {
        Feed feed = sampleFeed(Visibility.PAID_SUBSCRIBER);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(99L)));
        given(subscriptionLevelChecker.getLevel(6L, 99L)).willReturn(null);
        given(memberRepository.findById(99L)).willReturn(Optional.of(sampleMember(99L)));
        given(categoryRepository.findById(1L)).willReturn(Optional.of(sampleCategory(1L)));

        FeedDetailResponse result = feedService.getFeed(1L, 6L);

        assertThat(result.locked()).isTrue();
        assertThat(result.content()).isNull();
        assertThat(result.requiredLevel()).isEqualTo("PAID_SUBSCRIBER");
    }

    @Test
    @DisplayName("[LYJ-002] 존재하지 않는 피드 조회 시 FEED_NOT_FOUND 예외가 발생한다")
    void getFeed_notFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.getFeed(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-003 피드 수정
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-003] 피드를 수정하면 수정된 feedId를 반환하고 필드가 변경된다")
    void updateFeed_success() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.FREE_SUBSCRIBER);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(1L))); // creatorId=memberId=1L

        Long result = feedService.updateFeed(1L, 1L, request);

        assertThat(result).isEqualTo(1L);
        assertThat(feed.getTitle()).isEqualTo("새제목");
        assertThat(feed.getContent()).isEqualTo("새내용");
        assertThat(feed.getVisibility()).isEqualTo(Visibility.FREE_SUBSCRIBER);
    }

    @Test
    @DisplayName("[LYJ-003] 다른 크리에이터의 피드 수정 시 FEED_FORBIDDEN 예외 발생")
    void updateFeed_notOwner_forbidden() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.PUBLIC);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(99L))); // creatorId=99L ≠ memberId=1L

        assertThatThrownBy(() -> feedService.updateFeed(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    @Test
    @DisplayName("[LYJ-003] 존재하지 않는 피드 수정 시 FEED_NOT_FOUND 예외가 발생한다")
    void updateFeed_notFound() {
        FeedUpdateRequest request = new FeedUpdateRequest("새제목", "새내용", Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.updateFeed(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-004 피드 프로젝트 이동
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-004] 피드의 프로젝트를 이동하면 feedId를 반환하고 projectId가 변경된다")
    void moveFeedProject_success() {
        FeedMoveProjectRequest request = new FeedMoveProjectRequest(2L);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L)) // 현재 프로젝트
                .willReturn(Optional.of(sampleProject(1L)));
        given(projectRepository.findByIdAndDeletedFalse(2L)) // 이동할 프로젝트
                .willReturn(Optional.of(sampleProject(1L)));

        Long result = feedService.moveFeedProject(1L, 1L, request);

        assertThat(result).isEqualTo(1L);
        assertThat(feed.getProjectId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("[LYJ-004] 다른 크리에이터의 피드 이동 시 FEED_FORBIDDEN 예외 발생")
    void moveFeedProject_notOwner_forbidden() {
        FeedMoveProjectRequest request = new FeedMoveProjectRequest(2L);
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(99L))); // creatorId=99L ≠ memberId=1L

        assertThatThrownBy(() -> feedService.moveFeedProject(1L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    @Test
    @DisplayName("[LYJ-004] 존재하지 않는 피드 프로젝트 이동 시 FEED_NOT_FOUND 예외가 발생한다")
    void moveFeedProject_notFound() {
        FeedMoveProjectRequest request = new FeedMoveProjectRequest(2L);
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.moveFeedProject(999L, 1L, request))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-005 피드 소프트 삭제
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-005] 피드를 삭제하면 deleted=true로 변경된다")
    void deleteFeed_success() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(1L)));

        feedService.deleteFeed(1L, 1L);

        assertThat(feed.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-005] 다른 크리에이터의 피드 삭제 시 FEED_FORBIDDEN 예외 발생")
    void deleteFeed_notOwner_forbidden() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(feed));
        given(projectRepository.findByIdAndDeletedFalse(1L))
                .willReturn(Optional.of(sampleProject(99L)));

        assertThatThrownBy(() -> feedService.deleteFeed(1L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
    }

    @Test
    @DisplayName("[LYJ-005] 존재하지 않는 피드 삭제 시 FEED_NOT_FOUND 예외가 발생한다")
    void deleteFeed_notFound() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.deleteFeed(999L, 1L))
                .isInstanceOf(CustomException.class)
                .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                        .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-007 회원 홈 피드
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-007] 구독이 없으면 본인 피드만 포함해서 홈 피드를 반환한다")
    void getHomeFeed_noSubscription_returnsFeeds() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(subscriptionLevelChecker.getActiveSubscriptionLevels(1L)).willReturn(Map.of());
        given(feedRepository.findHomeFeeds(
                eq(1L), any(List.class), any(List.class), isNull(), any(List.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(feed)));
        given(projectRepository.findAllById(any())).willReturn(List.of(sampleProject(1L)));
        given(memberRepository.findAllById(any())).willReturn(List.of(sampleMember(1L)));
        given(categoryRepository.findAllById(any())).willReturn(List.of(sampleCategory(1L)));

        Slice<FeedSummaryResponse> result = feedService.getHomeFeed(1L, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(subscriptionLevelChecker).getActiveSubscriptionLevels(1L);
    }

    @Test
    @DisplayName("[LYJ-007] 구독 피드가 없으면 빈 목록을 반환한다")
    void getHomeFeed_noSubscription_empty() {
        given(subscriptionLevelChecker.getActiveSubscriptionLevels(1L)).willReturn(Map.of());
        given(feedRepository.findHomeFeeds(
                eq(1L), any(List.class), any(List.class), isNull(), any(List.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        Slice<FeedSummaryResponse> result = feedService.getHomeFeed(1L, null, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // LYJ-008 전체 공개 탐색 피드
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-008] 탐색 피드 조회 시 PUBLIC 피드를 반환한다")
    void getExploreFeeds_returnsPublicFeeds() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findExploreFeeds(isNull(), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(feed)));
        given(projectRepository.findAllById(any())).willReturn(List.of(sampleProject(1L)));
        given(memberRepository.findAllById(any())).willReturn(List.of(sampleMember(1L)));
        given(categoryRepository.findAllById(any())).willReturn(List.of(sampleCategory(1L)));

        Slice<FeedSummaryResponse> result = feedService.getExploreFeeds(null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).visibility()).isEqualTo(Visibility.PUBLIC);
    }

    @Test
    @DisplayName("[LYJ-008] 결과가 없으면 빈 리스트를 반환한다")
    void getExploreFeeds_noResult_returnsEmpty() {
        given(feedRepository.findExploreFeeds(isNull(), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        Slice<FeedSummaryResponse> result = feedService.getExploreFeeds(null, Pageable.unpaged());

        assertThat(result.getContent()).isEmpty();
    }

    // ---------------------------------------------------------------
    // LYJ-009 크리에이터별 피드 조회
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-009] 비회원은 구독 레벨 조회 없이 PUBLIC 피드만 반환한다")
    void getCreatorFeeds_nonMember_returnsPublicOnly() {
        Feed feed = sampleFeed(Visibility.PUBLIC);
        given(feedRepository.findByCreatorIdAndVisibilityIn(
                eq(10L), any(List.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(feed)));
        given(projectRepository.findAllById(any())).willReturn(List.of(sampleProject(1L)));
        given(memberRepository.findAllById(any())).willReturn(List.of(sampleMember(1L)));
        given(categoryRepository.findAllById(any())).willReturn(List.of(sampleCategory(1L)));

        Slice<FeedSummaryResponse> result = feedService.getCreatorFeeds(10L, null, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
        verify(subscriptionLevelChecker, never()).getLevel(any(), any());
    }

    @Test
    @DisplayName("[LYJ-009] 회원은 구독 레벨 조회 후 피드를 반환한다")
    void getCreatorFeeds_member_callsLevelChecker() {
        given(subscriptionLevelChecker.getLevel(1L, 10L)).willReturn("FREE");
        given(feedRepository.findByCreatorIdAndVisibilityIn(
                eq(10L), any(List.class), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of()));

        feedService.getCreatorFeeds(10L, 1L, Pageable.unpaged());

        verify(subscriptionLevelChecker).getLevel(1L, 10L);
    }

    // ---------------------------------------------------------------
    // LYJ-010 내 피드 목록
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-010] 크리에이터 본인의 피드 목록을 반환한다")
    void getMyFeeds_returnsCreatorFeeds() {
        Feed feed = sampleFeed(Visibility.PAID_SUBSCRIBER);
        given(feedRepository.findByCreatorId(eq(1L), any(Pageable.class)))
                .willReturn(new SliceImpl<>(List.of(feed)));
        given(projectRepository.findAllById(any())).willReturn(List.of(sampleProject(1L)));
        given(memberRepository.findAllById(any())).willReturn(List.of(sampleMember(1L)));
        given(categoryRepository.findAllById(any())).willReturn(List.of(sampleCategory(1L)));

        Slice<FeedSummaryResponse> result = feedService.getMyFeeds(1L, Pageable.unpaged());

        assertThat(result.getContent()).hasSize(1);
    }

    // ---------------------------------------------------------------
    // LYJ-012 피드 첨부파일 추가
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-012] 소유자가 이미지 파일을 첨부하면 AttachmentResponse를 반환한다")
    void addAttachment_imageFile_success() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());
        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_photo.jpg",
            "https://s3.amazonaws.com/feeds/1/attachments/uuid_photo.jpg", "photo.jpg");
        given(objectStorage.upload("feeds/1/attachments", file)).willReturn(stored);

        FeedAttachment saved = sampleUploadAttachment(AttachmentType.IMAGE, stored, "image/jpeg");
        given(feedAttachmentRepository.save(any(FeedAttachment.class))).willReturn(saved);

        AttachmentResponse result = feedService.addAttachment(1L, 1L, file, new AddAttachmentRequest(null));

        assertThat(result.type()).isEqualTo(AttachmentType.IMAGE);
        assertThat(result.originalName()).isEqualTo("photo.jpg");
        verify(objectStorage).upload("feeds/1/attachments", file);
    }

    @Test
    @DisplayName("[LYJ-012] 소유자가 VIDEO_LINK를 첨부하면 S3 업로드 없이 저장된다")
    void addAttachment_videoLink_noS3Upload() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));

        FeedAttachment saved = sampleVideoLinkAttachment("https://youtu.be/abc");
        given(feedAttachmentRepository.save(any(FeedAttachment.class))).willReturn(saved);

        feedService.addAttachment(1L, 1L, null, new AddAttachmentRequest("https://youtu.be/abc"));

        // S3 업로드 호출 없음 확인
        verify(objectStorage, never()).upload(any(), any());
        verify(feedAttachmentRepository).save(any(FeedAttachment.class));
    }

    @Test
    @DisplayName("[LYJ-012] 소유자가 아닌 회원이 첨부파일 추가 시 FEED_FORBIDDEN 예외 발생")
    void addAttachment_notOwner_forbidden() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L))); // creatorId=99, 요청자=1

        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> feedService.addAttachment(1L, 1L, file, new AddAttachmentRequest(null)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
        verify(objectStorage, never()).upload(any(), any());
    }

    @Test
    @DisplayName("[LYJ-012] 피드가 없으면 FEED_NOT_FOUND 예외 발생")
    void addAttachment_feedNotFound_throws() {
        given(feedRepository.findByIdAndDeletedFalse(999L)).willReturn(Optional.empty());
        MockMultipartFile file = new MockMultipartFile("file", "photo.jpg", "image/jpeg", "data".getBytes());

        assertThatThrownBy(() -> feedService.addAttachment(999L, 1L, file, new AddAttachmentRequest(null)))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FEED_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-013 피드 첨부파일 삭제
    // ---------------------------------------------------------------
    @Test
    @DisplayName("[LYJ-013] 소유자가 S3 첨부파일을 삭제하면 S3 삭제 + 소프트 삭제가 호출된다")
    void deleteAttachment_withStorageKey_deletesS3AndSoftDeletes() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));

        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_photo.jpg", "https://...", "photo.jpg");
        FeedAttachment attachment = sampleUploadAttachment(AttachmentType.IMAGE, stored, "image/jpeg");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));

        feedService.deleteAttachment(1L, 10L, 1L);

        verify(objectStorage).delete("feeds/1/attachments/uuid_photo.jpg");
        assertThat(attachment.isDeleted()).isTrue();
        assertThat(attachment.getDeletedAt()).isNotNull();
    }

    @Test
    @DisplayName("[LYJ-013] VIDEO_LINK 첨부파일 삭제 시 S3 삭제 없이 소프트 삭제만 된다")
    void deleteAttachment_videoLink_noS3Delete() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));

        FeedAttachment attachment = sampleVideoLinkAttachment("https://youtu.be/abc");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));

        feedService.deleteAttachment(1L, 10L, 1L);

        // VIDEO_LINK는 storageKey=null → S3 삭제 호출 없음
        verify(objectStorage, never()).delete(any());
        assertThat(attachment.isDeleted()).isTrue();
    }

    @Test
    @DisplayName("[LYJ-013] 소유자가 아닌 회원이 삭제 시 FEED_FORBIDDEN 예외 발생")
    void deleteAttachment_notOwner_forbidden() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(99L))); // creatorId=99, 요청자=1

        assertThatThrownBy(() -> feedService.deleteAttachment(1L, 10L, 1L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FEED_FORBIDDEN));
        verify(objectStorage, never()).delete(any());
    }

    @Test
    @DisplayName("[LYJ-013] 첨부파일 ID가 없거나 이미 삭제된 경우 ATTACHMENT_NOT_FOUND 예외 발생")
    void deleteAttachment_notFound_throws() {
        given(feedRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L)).willReturn(Optional.of(sampleProject(1L)));
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(999L, 1L))
            .willReturn(Optional.empty());

        assertThatThrownBy(() -> feedService.deleteAttachment(1L, 999L, 1L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.ATTACHMENT_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // LYJ-014 첨부파일 다운로드 URL 조회
    // ---------------------------------------------------------------

    @Test
    @DisplayName("[LYJ-014] PUBLIC 피드 + IMAGE 첨부파일 → 비로그인도 Presigned URL 반환")
    void getDownloadUrl_publicFeed_image_anonymousOk() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));

        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_photo.jpg", "https://...", "photo.jpg");
        FeedAttachment attachment = sampleUploadAttachment(AttachmentType.IMAGE, stored, "image/jpeg");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));
        given(objectStorage.generatePresignedUrl("feeds/1/attachments/uuid_photo.jpg", 10))
            .willReturn("https://s3.amazonaws.com/...?X-Amz-Signature=abc");

        // when — 비로그인(memberId=null)
        DownloadResponse result = feedService.getDownloadUrl(1L, 10L, null);

        // then
        assertThat(result.type()).isEqualTo(AttachmentType.IMAGE);
        assertThat(result.downloadUrl()).contains("X-Amz-Signature");
        assertThat(result.expiresInMinutes()).isEqualTo(10);
        verify(objectStorage).generatePresignedUrl("feeds/1/attachments/uuid_photo.jpg", 10);
    }

    @Test
    @DisplayName("[LYJ-014] VIDEO_LINK → generatePresignedUrl 호출 없이 외부 URL 반환")
    void getDownloadUrl_videoLink_returnsExternalUrl() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));

        FeedAttachment attachment = sampleVideoLinkAttachment("https://youtu.be/abc");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));

        // when
        DownloadResponse result = feedService.getDownloadUrl(1L, 10L, null);

        // then
        assertThat(result.type()).isEqualTo(AttachmentType.VIDEO_LINK);
        assertThat(result.downloadUrl()).isEqualTo("https://youtu.be/abc");
        assertThat(result.expiresInMinutes()).isNull();
        verify(objectStorage, never()).generatePresignedUrl(any(), anyInt());
    }

    @Test
    @DisplayName("[LYJ-014] FREE_SUBSCRIBER 피드 + FREE 구독자 → 다운로드 가능")
    void getDownloadUrl_freeSubscriberFeed_freeSubscriber_ok() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.FREE_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));
        given(subscriptionLevelChecker.getLevel(5L, 10L)).willReturn("FREE");

        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_file.pdf", "https://...", "file.pdf");
        FeedAttachment attachment = sampleUploadAttachment(AttachmentType.FILE, stored, "application/pdf");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));
        given(objectStorage.generatePresignedUrl("feeds/1/attachments/uuid_file.pdf", 10))
            .willReturn("https://s3.amazonaws.com/...?X-Amz-Signature=xyz");

        // when
        DownloadResponse result = feedService.getDownloadUrl(1L, 10L, 5L);

        // then
        assertThat(result.type()).isEqualTo(AttachmentType.FILE);
        assertThat(result.originalName()).isEqualTo("file.pdf");
    }

    @Test
    @DisplayName("[LYJ-014] PAID_SUBSCRIBER 피드 + PAID 구독자 → 다운로드 가능")
    void getDownloadUrl_paidSubscriberFeed_paidSubscriber_ok() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PAID_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));
        given(subscriptionLevelChecker.getLevel(5L, 10L)).willReturn("PAID");

        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_file.pdf", "https://...", "file.pdf");
        FeedAttachment attachment = sampleUploadAttachment(AttachmentType.FILE, stored, "application/pdf");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));
        given(objectStorage.generatePresignedUrl(any(), eq(10))).willReturn("https://presigned-url");

        // when
        DownloadResponse result = feedService.getDownloadUrl(1L, 10L, 5L);

        // then
        assertThat(result.downloadUrl()).isEqualTo("https://presigned-url");
    }

    @Test
    @DisplayName("[LYJ-014] PAID_SUBSCRIBER 피드 + 소유자 → 다운로드 가능")
    void getDownloadUrl_paidSubscriberFeed_owner_ok() {
        // given — 요청자 ID == 프로젝트 creatorId == 10L
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PAID_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));

        StoredObject stored = new StoredObject("feeds/1/attachments/uuid_photo.jpg", "https://...", "photo.jpg");
        FeedAttachment attachment = sampleUploadAttachment(AttachmentType.IMAGE, stored, "image/jpeg");
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(10L, 1L))
            .willReturn(Optional.of(attachment));
        given(objectStorage.generatePresignedUrl(any(), eq(10))).willReturn("https://presigned-url");

        // when — memberId == creatorId == 10L
        DownloadResponse result = feedService.getDownloadUrl(1L, 10L, 10L);

        // then — 소유자는 구독 레벨 조회 없이 접근 가능
        assertThat(result.downloadUrl()).isEqualTo("https://presigned-url");
        verify(subscriptionLevelChecker, never()).getLevel(any(), any());
    }

    @Test
    @DisplayName("[LYJ-014] FREE_SUBSCRIBER 피드 + 비구독자 → FREE_SUBSCRIPTION_REQUIRED 예외")
    void getDownloadUrl_freeSubscriberFeed_nonSubscriber_throws() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.FREE_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));
        given(subscriptionLevelChecker.getLevel(5L, 10L)).willReturn(null); // 비구독

        // when & then
        assertThatThrownBy(() -> feedService.getDownloadUrl(1L, 10L, 5L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.FREE_SUBSCRIPTION_REQUIRED));
        verify(objectStorage, never()).generatePresignedUrl(any(), anyInt());
    }

    @Test
    @DisplayName("[LYJ-014] PAID_SUBSCRIBER 피드 + FREE 구독자 → PAID_SUBSCRIPTION_REQUIRED 예외")
    void getDownloadUrl_paidSubscriberFeed_freeSubscriber_throws() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PAID_SUBSCRIBER)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));
        given(subscriptionLevelChecker.getLevel(5L, 10L)).willReturn("FREE"); // 무료구독자

        // when & then
        assertThatThrownBy(() -> feedService.getDownloadUrl(1L, 10L, 5L))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.PAID_SUBSCRIPTION_REQUIRED));
        verify(objectStorage, never()).generatePresignedUrl(any(), anyInt());
    }

    @Test
    @DisplayName("[LYJ-014] 첨부파일이 없거나 소프트 삭제된 경우 ATTACHMENT_NOT_FOUND 예외")
    void getDownloadUrl_attachmentNotFound_throws() {
        // given
        given(feedRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleFeed(Visibility.PUBLIC)));
        given(projectRepository.findByIdAndDeletedFalse(1L))
            .willReturn(Optional.of(sampleProject(10L)));
        given(feedAttachmentRepository.findByIdAndFeedIdAndDeletedFalse(999L, 1L))
            .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> feedService.getDownloadUrl(1L, 999L, null))
            .isInstanceOf(CustomException.class)
            .satisfies(e -> assertThat(((CustomException) e).getErrorCode())
                .isEqualTo(FeedErrorCode.ATTACHMENT_NOT_FOUND));
    }

    // ---------------------------------------------------------------
    // 헬퍼
    // ---------------------------------------------------------------
    private Feed sampleFeed(Visibility visibility) {
        Feed feed = Feed.create(1L, "제목", "내용", visibility);
        ReflectionTestUtils.setField(feed, "id", 1L);
        ReflectionTestUtils.setField(feed, "createdAt", Instant.parse("2026-07-14T00:00:00Z"));
        ReflectionTestUtils.setField(feed, "updatedAt", Instant.parse("2026-07-14T00:00:00Z"));
        return feed;
    }

    private Project sampleProject(Long creatorId) {
        Project project = Project.builder()
                .creatorId(creatorId)
                .categoryId(1L)
                .title("테스트 프로젝트")
                .build();
        ReflectionTestUtils.setField(project, "id", 1L);
        return project;
    }

    private Member sampleMember(Long id) {
        Member member = Member.createLocalMember("test@test.com", "password", "테스터");
        ReflectionTestUtils.setField(member, "id", id);
        return member;
    }

    private Category sampleCategory(Long id) {
        Category category = Category.builder().name("테스트카테고리").build();
        ReflectionTestUtils.setField(category, "id", id);
        return category;
    }

    private Poll samplePoll(Long feedId, Long id) {
        Poll poll = Poll.create(feedId, "질문", Instant.now().plusSeconds(3600));
        ReflectionTestUtils.setField(poll, "id", id);
        return poll;
    }

    private FeedAttachment sampleUploadAttachment(AttachmentType type, StoredObject stored, String mimeType) {
        FeedAttachment a = FeedAttachment.ofUpload(1L, type, stored, 1024L, mimeType);
        ReflectionTestUtils.setField(a, "id", 10L);
        ReflectionTestUtils.setField(a, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
        return a;
    }

    private FeedAttachment sampleVideoLinkAttachment(String url) {
        FeedAttachment a = FeedAttachment.ofVideoLink(1L, url);
        ReflectionTestUtils.setField(a, "id", 10L);
        ReflectionTestUtils.setField(a, "createdAt", Instant.parse("2026-07-15T00:00:00Z"));
        return a;
    }

}
