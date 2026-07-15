package com.white.handdam.board.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.white.handdam.board.dto.request.CreateBoardPostRequest;
import com.white.handdam.board.dto.request.UpdateBoardPostRequest;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostImage;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.exception.BoardErrorCode;
import com.white.handdam.board.repository.BoardPostImageRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.global.exception.CustomException;
import com.white.handdam.global.exception.ErrorCode;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.web.multipart.MultipartFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class BoardPostServiceTest {

	@Mock
	private BoardPostRepository boardPostRepository;

	@Mock
	private BoardPostImageRepository boardPostImageRepository;

	@Mock
	private PaidSubscriptionChecker paidSubscriptionChecker;

	@Mock
	private ObjectStorage objectStorage;

	@InjectMocks
	private BoardPostService boardPostService;

	private Pageable pageable;

	@BeforeEach
	void setUp() {
		pageable = PageRequest.of(0, 20);
	}

	@Test
	@DisplayName("크리에이터 본인은 목록을 조회할 수 있다")
	void creatorCanAccess() {
		Long creatorId = 1L;
		BoardPost post = samplePost(creatorId, 2L);
		given(boardPostRepository.findByCreator(eq(creatorId), isNull(), isNull(), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(post)));

		Page<BoardPostResponse> result =
			boardPostService.getPostsByCreator(creatorId, creatorId, null, null, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().title()).isEqualTo("테스트 제목");
		verifyNoInteractions(paidSubscriptionChecker);
	}

	@Test
	@DisplayName("활성 유료 구독자는 목록을 조회할 수 있다")
	void paidSubscriberCanAccess() {
		Long creatorId = 1L;
		Long subscriberId = 99L;
		given(paidSubscriptionChecker.hasActivePaidSubscription(subscriberId, creatorId)).willReturn(true);
		given(boardPostRepository.findByCreator(eq(creatorId), isNull(), isNull(), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(samplePost(creatorId, subscriberId))));

		Page<BoardPostResponse> result =
			boardPostService.getPostsByCreator(creatorId, subscriberId, null, null, pageable);

		assertThat(result.getContent()).hasSize(1);
		verify(paidSubscriptionChecker).hasActivePaidSubscription(subscriberId, creatorId);
	}

	@Test
	@DisplayName("권한이 없으면 목록 조회 시 SUBSCRIPTION_REQUIRED 예외가 발생한다")
	void deniedWithoutPermission() {
		Long creatorId = 1L;
		Long strangerId = 7L;
		given(paidSubscriptionChecker.hasActivePaidSubscription(strangerId, creatorId)).willReturn(false);

		assertThatThrownBy(() ->
			boardPostService.getPostsByCreator(creatorId, strangerId, null, null, pageable)
		).satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardPostRepository, never()).findByCreator(any(), any(), any(), any());
	}

	@Test
	@DisplayName("유료 구독자는 제목·이미지와 함께 게시글을 작성할 수 있다")
	void paidSubscriberCanCreatePostWithImages() {
		Long creatorId = 1L;
		Long subscriberId = 99L;
		CreateBoardPostRequest request = new CreateBoardPostRequest(
			"재료 질문",
			BoardPostType.QUESTION,
			"재료가 궁금해요"
		);
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"a.jpg",
			"image/jpeg",
			new byte[] {1, 2, 3}
		);

		given(paidSubscriptionChecker.hasActivePaidSubscription(subscriberId, creatorId)).willReturn(true);
		given(objectStorage.upload(eq("premium-board/1"), eq(image)))
			.willReturn(new StoredObject(
				"premium-board/1/uuid_a.jpg",
				"http://localhost:4566/handdam-local/premium-board/1/uuid_a.jpg",
				"a.jpg"
			));
		given(boardPostRepository.save(any(BoardPost.class))).willAnswer(invocation -> {
			BoardPost post = invocation.getArgument(0);
			ReflectionTestUtils.setField(post, "id", 10L);
			ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-07-12T00:00:00Z"));
			ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2026-07-12T00:00:00Z"));
			return post;
		});
		given(boardPostImageRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<BoardPostImage> images = invocation.getArgument(0);
			ReflectionTestUtils.setField(images.getFirst(), "id", 100L);
			ReflectionTestUtils.setField(images.getFirst(), "createdAt", Instant.parse("2026-07-12T00:00:00Z"));
			return images;
		});

		BoardPostResponse result = boardPostService.createPost(
			creatorId,
			subscriberId,
			request,
			List.of(image)
		);

		assertThat(result.title()).isEqualTo("재료 질문");
		assertThat(result.images()).hasSize(1);
		assertThat(result.images().getFirst().fileSize()).isEqualTo(3L);
		assertThat(result.images().getFirst().mimeType()).isEqualTo("image/jpeg");
		assertThat(result.images().getFirst().storageKey()).isEqualTo("premium-board/1/uuid_a.jpg");

		ArgumentCaptor<BoardPost> postCaptor = ArgumentCaptor.forClass(BoardPost.class);
		verify(boardPostRepository).save(postCaptor.capture());
		assertThat(postCaptor.getValue().getTitle()).isEqualTo("재료 질문");
		verify(objectStorage).upload("premium-board/1", image);
	}

	@Test
	@DisplayName("크리에이터 본인은 이미지 없이 게시글을 작성할 수 있다")
	void creatorCanCreatePostWithoutImages() {
		Long creatorId = 1L;
		CreateBoardPostRequest request = new CreateBoardPostRequest(
			"공지",
			BoardPostType.GENERAL,
			"본문"
		);
		given(boardPostRepository.save(any(BoardPost.class))).willAnswer(invocation -> {
			BoardPost post = invocation.getArgument(0);
			ReflectionTestUtils.setField(post, "id", 11L);
			ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-07-12T00:00:00Z"));
			ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2026-07-12T00:00:00Z"));
			return post;
		});

		BoardPostResponse result = boardPostService.createPost(creatorId, creatorId, request, null);

		assertThat(result.id()).isEqualTo(11L);
		assertThat(result.images()).isEmpty();
		verifyNoInteractions(objectStorage);
		verify(boardPostImageRepository, never()).saveAll(anyList());
		verifyNoInteractions(paidSubscriptionChecker);
	}

	@Test
	@DisplayName("권한 없는 사용자는 게시글을 작성할 수 없다")
	void deniedCannotCreatePost() {
		Long creatorId = 1L;
		Long strangerId = 7L;
		CreateBoardPostRequest request = new CreateBoardPostRequest(
			"안녕하세요",
			BoardPostType.GENERAL,
			"본문"
		);
		given(paidSubscriptionChecker.hasActivePaidSubscription(strangerId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.createPost(creatorId, strangerId, request, null))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardPostRepository, never()).save(any());
		verifyNoInteractions(objectStorage);
	}

	@Test
	@DisplayName("작성자는 본인 게시글 상세를 조회할 수 있다")
	void authorCanGetPostDetail() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(10L)).willReturn(List.of());

		BoardPostResponse result = boardPostService.getPost(10L, authorId);

		assertThat(result.id()).isEqualTo(10L);
		assertThat(result.memberId()).isEqualTo(authorId);
		assertThat(result.images()).isEmpty();
		verifyNoInteractions(paidSubscriptionChecker);
	}

	@Test
	@DisplayName("게시판 크리에이터는 게시글 상세를 조회할 수 있다")
	void creatorCanGetPostDetail() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(10L)).willReturn(List.of());

		BoardPostResponse result = boardPostService.getPost(10L, creatorId);

		assertThat(result.id()).isEqualTo(10L);
		verifyNoInteractions(paidSubscriptionChecker);
	}

	@Test
	@DisplayName("유료 구독자는 게시글 상세와 이미지를 조회할 수 있다")
	void paidSubscriberCanGetPostDetailWithImages() {
		Long creatorId = 1L;
		Long subscriberId = 99L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		BoardPostImage image = BoardPostImage.builder()
			.boardPost(post)
			.url("http://localhost:4566/handdam-local/a.jpg")
			.storageKey("premium-board/1/a.jpg")
			.originalName("a.jpg")
			.fileSize(10L)
			.mimeType("image/jpeg")
			.orderIndex(0)
			.build();
		ReflectionTestUtils.setField(image, "id", 100L);
		ReflectionTestUtils.setField(image, "createdAt", Instant.parse("2026-07-12T00:00:00Z"));

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(subscriberId, creatorId)).willReturn(true);
		given(boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(10L)).willReturn(List.of(image));

		BoardPostResponse result = boardPostService.getPost(10L, subscriberId);

		assertThat(result.id()).isEqualTo(10L);
		assertThat(result.images()).hasSize(1);
		assertThat(result.images().getFirst().storageKey()).isEqualTo("premium-board/1/a.jpg");
	}

	@Test
	@DisplayName("삭제된 게시글은 상세 조회 시 RESOURCE_NOT_FOUND를 반환한다")
	void deletedPostReturnsNotFound() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardPostService.getPost(10L, 1L))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));

		verify(boardPostImageRepository, never()).findByBoardPostIdOrderByOrderIndexAsc(any());
	}

	@Test
	@DisplayName("권한 없는 사용자는 게시글 상세를 조회할 수 없다")
	void deniedCannotGetPostDetail() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 7L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(strangerId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.getPost(10L, strangerId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardPostImageRepository, never()).findByBoardPostIdOrderByOrderIndexAsc(any());
	}

	@Test
	@DisplayName("작성자는 답변 대기 게시글을 수정할 수 있다")
	void authorCanUpdateWaitingPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		UpdateBoardPostRequest request = new UpdateBoardPostRequest(
			"수정된 제목",
			BoardPostType.GENERAL,
			"수정된 본문"
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(10L)).willReturn(List.of());

		BoardPostResponse result = boardPostService.updatePost(10L, authorId, request);

		assertThat(result.title()).isEqualTo("수정된 제목");
		assertThat(result.type()).isEqualTo(BoardPostType.GENERAL);
		assertThat(result.content()).isEqualTo("수정된 본문");
		assertThat(post.getTitle()).isEqualTo("수정된 제목");
	}

	@Test
	@DisplayName("공식 답변이 달린 게시글은 수정할 수 없다")
	void answeredPostCannotBeUpdated() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		ReflectionTestUtils.setField(post, "status", BoardPostStatus.ANSWERED);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		UpdateBoardPostRequest request = new UpdateBoardPostRequest(
			"수정된 제목",
			BoardPostType.GENERAL,
			"수정된 본문"
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.updatePost(10L, authorId, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_ALREADY_ANSWERED));
	}

	@Test
	@DisplayName("작성자가 아니면 게시글을 수정할 수 없다")
	void nonAuthorCannotUpdatePost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		UpdateBoardPostRequest request = new UpdateBoardPostRequest(
			"수정된 제목",
			BoardPostType.GENERAL,
			"수정된 본문"
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.updatePost(10L, subscriberId, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_EDIT_FORBIDDEN));

		assertThat(post.getTitle()).isEqualTo("테스트 제목");
	}

	@Test
	@DisplayName("삭제된 게시글은 수정 시 RESOURCE_NOT_FOUND를 반환한다")
	void deletedPostCannotBeUpdated() {
		UpdateBoardPostRequest request = new UpdateBoardPostRequest(
			"수정된 제목",
			BoardPostType.GENERAL,
			"수정된 본문"
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardPostService.updatePost(10L, 5L, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));
	}

	@Test
	@DisplayName("작성자는 본인 게시글을 소프트 삭제할 수 있다")
	void authorCanSoftDeletePost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);

		boardPostService.deletePost(10L, authorId);

		assertThat(post.isDeleted()).isTrue();
		assertThat(post.getDeletedAt()).isNotNull();
	}

	@Test
	@DisplayName("작성자가 아니면 게시글을 삭제할 수 없다")
	void nonAuthorCannotDeletePost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long strangerId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.deletePost(10L, strangerId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_EDIT_FORBIDDEN));

		assertThat(post.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("삭제된 게시글은 삭제 시 RESOURCE_NOT_FOUND를 반환한다")
	void deletedPostCannotBeDeletedAgain() {
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardPostService.deletePost(10L, 5L))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_NOT_FOUND));
	}

	@Test
	@DisplayName("로그인한 회원은 내가 작성한 글 목록을 조회할 수 있다")
	void memberCanGetMyPosts() {
		Long memberId = 5L;
		BoardPost post = samplePost(1L, memberId);
		given(boardPostRepository.findByMember(eq(memberId), isNull(), isNull(), eq(pageable)))
			.willReturn(new PageImpl<>(List.of(post)));

		Page<BoardPostResponse> result =
			boardPostService.getMyPosts(memberId, null, null, pageable);

		assertThat(result.getContent()).hasSize(1);
		assertThat(result.getContent().getFirst().memberId()).isEqualTo(memberId);
	}

	@Test
	@DisplayName("비로그인은 내 작성글 목록을 조회할 수 없다")
	void guestCannotGetMyPosts() {
		assertThatThrownBy(() -> boardPostService.getMyPosts(null, null, null, pageable))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_LOGIN_REQUIRED));

		verify(boardPostRepository, never()).findByMember(any(), any(), any(), any());
	}

	@Test
	@DisplayName("작성자는 본인 게시글에 이미지를 추가할 수 있다")
	void authorCanAddImages() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"b.jpg",
			"image/jpeg",
			new byte[] {4, 5}
		);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findMaxOrderIndexByBoardPostId(10L))
			.willReturn(Optional.of(0));
		given(objectStorage.upload(eq("premium-board/1"), eq(image)))
			.willReturn(new StoredObject(
				"premium-board/1/uuid_b.jpg",
				"http://localhost:4566/handdam-local/premium-board/1/uuid_b.jpg",
				"b.jpg"
			));
		given(boardPostImageRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<BoardPostImage> images = invocation.getArgument(0);
			ReflectionTestUtils.setField(images.getFirst(), "id", 101L);
			ReflectionTestUtils.setField(images.getFirst(), "createdAt", Instant.parse("2026-07-13T00:00:00Z"));
			return images;
		});

		var result = boardPostService.addImages(10L, authorId, List.of(image));

		assertThat(result).hasSize(1);
		assertThat(result.getFirst().orderIndex()).isEqualTo(1);
		assertThat(result.getFirst().storageKey()).isEqualTo("premium-board/1/uuid_b.jpg");
		assertThat(result.getFirst().fileSize()).isEqualTo(2L);
		verify(objectStorage).upload("premium-board/1", image);
	}

	@Test
	@DisplayName("작성자가 아니면 이미지를 추가할 수 없다")
	void nonAuthorCannotAddImages() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"b.jpg",
			"image/jpeg",
			new byte[] {1}
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.addImages(10L, subscriberId, List.of(image)))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_EDIT_FORBIDDEN));

		verifyNoInteractions(objectStorage);
		verify(boardPostImageRepository, never()).saveAll(anyList());
	}

	@Test
	@DisplayName("공식 답변된 게시글에는 이미지를 추가할 수 없다")
	void answeredPostCannotAddImages() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		ReflectionTestUtils.setField(post, "status", BoardPostStatus.ANSWERED);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"b.jpg",
			"image/jpeg",
			new byte[] {1}
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.addImages(10L, authorId, List.of(image)))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_IMAGE_ADD_NOT_ALLOWED));

		verifyNoInteractions(objectStorage);
	}

	@Test
	@DisplayName("이미지 파일이 없으면 BOARD_POST_IMAGE_REQUIRED를 반환한다")
	void emptyImagesRejected() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		given(boardPostImageRepository.findMaxOrderIndexByBoardPostId(10L))
			.willReturn(Optional.empty());

		assertThatThrownBy(() -> boardPostService.addImages(10L, authorId, List.of()))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_IMAGE_REQUIRED));

		verifyNoInteractions(objectStorage);
	}

	@Test
	@DisplayName("게시글 작성 시 이미지 원본 바이트가 스토리지에 실제로 저장된다")
	void createPostActuallyUploadsImageBytes() {
		RecordingObjectStorage realStorage = new RecordingObjectStorage();
		BoardPostService service = new BoardPostService(
			boardPostRepository,
			boardPostImageRepository,
			paidSubscriptionChecker,
			realStorage
		);

		Long creatorId = 1L;
		Long subscriberId = 99L;
		byte[] imageBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 0x01, 0x02, 0x03};
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"real-upload.jpg",
			"image/jpeg",
			imageBytes
		);
		CreateBoardPostRequest request = new CreateBoardPostRequest(
			"실제 업로드 테스트",
			BoardPostType.QUESTION,
			"바이트 검증"
		);

		given(paidSubscriptionChecker.hasActivePaidSubscription(subscriberId, creatorId)).willReturn(true);
		given(boardPostRepository.save(any(BoardPost.class))).willAnswer(invocation -> {
			BoardPost post = invocation.getArgument(0);
			ReflectionTestUtils.setField(post, "id", 20L);
			ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-07-14T00:00:00Z"));
			ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2026-07-14T00:00:00Z"));
			return post;
		});
		given(boardPostImageRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<BoardPostImage> images = invocation.getArgument(0);
			ReflectionTestUtils.setField(images.getFirst(), "id", 200L);
			ReflectionTestUtils.setField(images.getFirst(), "createdAt", Instant.parse("2026-07-14T00:00:00Z"));
			return images;
		});

		BoardPostResponse result = service.createPost(
			creatorId,
			subscriberId,
			request,
			List.of(image)
		);

		String storageKey = result.images().getFirst().storageKey();
		assertThat(result.images()).hasSize(1);
		assertThat(result.images().getFirst().originalName()).isEqualTo("real-upload.jpg");
		assertThat(result.images().getFirst().fileSize()).isEqualTo(imageBytes.length);
		assertThat(realStorage.size()).isEqualTo(1);
		assertThat(realStorage.getBytes(storageKey)).isEqualTo(imageBytes);
		assertThat(realStorage.getBytes(storageKey)).isNotEqualTo(new byte[] {1, 2, 3});
	}

	@Test
	@DisplayName("이미지 추가 시 원본 바이트가 스토리지에 실제로 저장된다")
	void addImagesActuallyUploadsImageBytes() {
		RecordingObjectStorage realStorage = new RecordingObjectStorage();
		BoardPostService service = new BoardPostService(
			boardPostRepository,
			boardPostImageRepository,
			paidSubscriptionChecker,
			realStorage
		);

		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		byte[] imageBytes = {10, 20, 30, 40, 50};
		MockMultipartFile image = new MockMultipartFile(
			"images",
			"added.png",
			"image/png",
			imageBytes
		);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findMaxOrderIndexByBoardPostId(10L))
			.willReturn(Optional.empty());
		given(boardPostImageRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<BoardPostImage> images = invocation.getArgument(0);
			ReflectionTestUtils.setField(images.getFirst(), "id", 201L);
			ReflectionTestUtils.setField(images.getFirst(), "createdAt", Instant.parse("2026-07-14T01:00:00Z"));
			return images;
		});

		var result = service.addImages(10L, authorId, List.of(image));

		String storageKey = result.getFirst().storageKey();
		assertThat(result).hasSize(1);
		assertThat(result.getFirst().originalName()).isEqualTo("added.png");
		assertThat(result.getFirst().fileSize()).isEqualTo(imageBytes.length);
		assertThat(realStorage.getBytes(storageKey)).isEqualTo(imageBytes);
	}

	@Test
	@DisplayName("이미지 삭제 시 스토리지에 저장된 원본 바이트가 실제로 제거된다")
	void deleteImageActuallyRemovesStoredBytes() {
		RecordingObjectStorage realStorage = new RecordingObjectStorage();
		BoardPostService service = new BoardPostService(
			boardPostRepository,
			boardPostImageRepository,
			paidSubscriptionChecker,
			realStorage
		);

		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		byte[] imageBytes = {9, 8, 7, 6, 5};
		MockMultipartFile file = new MockMultipartFile(
			"images",
			"to-delete.jpg",
			"image/jpeg",
			imageBytes
		);

		// 먼저 스토리지에 실제 바이트를 올려 두고
		StoredObject stored = realStorage.upload("premium-board/1", file);
		BoardPostImage image = BoardPostImage.builder()
			.boardPost(post)
			.url(stored.url())
			.storageKey(stored.storageKey())
			.originalName(stored.originalName())
			.fileSize((long) imageBytes.length)
			.mimeType("image/jpeg")
			.orderIndex(0)
			.build();
		ReflectionTestUtils.setField(image, "id", 100L);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByIdAndBoardPostId(100L, 10L)).willReturn(Optional.of(image));

		assertThat(realStorage.size()).isEqualTo(1);
		assertThat(realStorage.getBytes(stored.storageKey())).isEqualTo(imageBytes);

		service.deleteImage(10L, 100L, authorId);

		assertThat(realStorage.getBytes(stored.storageKey())).isNull();
		assertThat(realStorage.size()).isEqualTo(0);
		verify(boardPostImageRepository).delete(image);
	}

	@Test
	@DisplayName("작성자는 본인 게시글의 이미지를 삭제할 수 있다")
	void authorCanDeleteImage() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		BoardPostImage image = BoardPostImage.builder()
			.boardPost(post)
			.url("http://localhost:4566/handdam-local/a.jpg")
			.storageKey("premium-board/1/uuid_a.jpg")
			.originalName("a.jpg")
			.fileSize(10L)
			.mimeType("image/jpeg")
			.orderIndex(0)
			.build();
		ReflectionTestUtils.setField(image, "id", 100L);

		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByIdAndBoardPostId(100L, 10L)).willReturn(Optional.of(image));

		boardPostService.deleteImage(10L, 100L, authorId);

		verify(objectStorage).delete("premium-board/1/uuid_a.jpg");
		verify(boardPostImageRepository).delete(image);
	}

	@Test
	@DisplayName("작성자가 아니면 이미지를 삭제할 수 없다")
	void nonAuthorCannotDeleteImage() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long subscriberId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.deleteImage(10L, 100L, subscriberId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_EDIT_FORBIDDEN));

		verifyNoInteractions(objectStorage);
		verify(boardPostImageRepository, never()).delete(any());
	}

	@Test
	@DisplayName("공식 답변된 게시글의 이미지는 삭제할 수 없다")
	void answeredPostCannotDeleteImage() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		ReflectionTestUtils.setField(post, "status", BoardPostStatus.ANSWERED);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));

		assertThatThrownBy(() -> boardPostService.deleteImage(10L, 100L, authorId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_POST_IMAGE_DELETE_NOT_ALLOWED));

		verifyNoInteractions(objectStorage);
		verify(boardPostImageRepository, never()).delete(any());
	}

	@Test
	@DisplayName("이미지가 없거나 다른 게시글 이미지면 RESOURCE_NOT_FOUND를 반환한다")
	void missingImageCannotBeDeleted() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(true);
		given(boardPostImageRepository.findByIdAndBoardPostId(999L, 10L)).willReturn(Optional.empty());

		assertThatThrownBy(() -> boardPostService.deleteImage(10L, 999L, authorId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_IMAGE_NOT_FOUND));

		verifyNoInteractions(objectStorage);
		verify(boardPostImageRepository, never()).delete(any());
	}


	@Test
	@DisplayName("구독 해지 후에도 본인 글 상세는 조회할 수 있다")
	void expiredAuthorCanGetOwnPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(10L)).willReturn(List.of());

		BoardPostResponse result = boardPostService.getPost(10L, authorId);

		assertThat(result.memberId()).isEqualTo(authorId);
		verifyNoInteractions(paidSubscriptionChecker);
	}

	@Test
	@DisplayName("구독 해지 후에는 타인 글 상세를 조회할 수 없다")
	void expiredUserCannotGetOthersPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		Long expiredId = 99L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(expiredId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.getPost(10L, expiredId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));
	}

	@Test
	@DisplayName("구독 해지 후에는 본인 글도 수정할 수 없다")
	void expiredAuthorCannotUpdateOwnPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		UpdateBoardPostRequest request = new UpdateBoardPostRequest(
			"수정된 제목",
			BoardPostType.GENERAL,
			"수정된 본문"
		);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.updatePost(10L, authorId, request))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));
	}

	@Test
	@DisplayName("구독 해지 후에는 본인 글도 삭제할 수 없다")
	void expiredAuthorCannotDeleteOwnPost() {
		Long creatorId = 1L;
		Long authorId = 5L;
		BoardPost post = samplePost(creatorId, authorId);
		given(boardPostRepository.findByIdAndDeletedFalse(10L)).willReturn(Optional.of(post));
		given(paidSubscriptionChecker.hasActivePaidSubscription(authorId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.deletePost(10L, authorId))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		assertThat(post.isDeleted()).isFalse();
	}

	@Test
	@DisplayName("구독 해지 후에는 새 글을 작성할 수 없다")
	void expiredUserCannotCreatePost() {
		Long creatorId = 1L;
		Long expiredId = 99L;
		CreateBoardPostRequest request = new CreateBoardPostRequest(
			"제목",
			BoardPostType.QUESTION,
			"본문"
		);
		given(paidSubscriptionChecker.hasActivePaidSubscription(expiredId, creatorId)).willReturn(false);

		assertThatThrownBy(() -> boardPostService.createPost(creatorId, expiredId, request, null))
			.satisfies(ex -> assertErrorCode(ex, BoardErrorCode.BOARD_SUBSCRIPTION_REQUIRED));

		verify(boardPostRepository, never()).save(any());
	}

	private static void assertErrorCode(Throwable thrown, ErrorCode expected) {
		assertThat(thrown).isInstanceOf(CustomException.class);
		assertThat(((CustomException) thrown).getErrorCode()).isEqualTo(expected);
	}

	/**
	 * mock URL 반환이 아니라, 업로드된 MultipartFile 바이트를 실제로 보관하는 테스트용 스토리지.
	 */
	private static final class RecordingObjectStorage implements ObjectStorage {

		private final Map<String, byte[]> store = new ConcurrentHashMap<>();

		@Override
		public StoredObject upload(String folder, MultipartFile file) {
			if (file == null || file.isEmpty()) {
				throw new IllegalArgumentException("이미지 파일이 비어 있습니다.");
			}
			String originalName = file.getOriginalFilename() == null ? "image" : file.getOriginalFilename();
			String storageKey = folder + "/" + UUID.randomUUID() + "_" + originalName;
			try {
				store.put(storageKey, file.getBytes());
			} catch (IOException e) {
				throw new IllegalStateException("이미지 업로드에 실패했습니다.", e);
			}
			return new StoredObject(storageKey, "http://memory/" + storageKey, originalName);
		}

		@Override
		public void delete(String storageKey) {
			store.remove(storageKey);
		}

		byte[] getBytes(String storageKey) {
			return store.get(storageKey);
		}

		int size() {
			return store.size();
		}
	}

	private BoardPost samplePost(Long creatorId, Long memberId) {
		BoardPost post = BoardPost.builder()
			.creatorId(creatorId)
			.memberId(memberId)
			.title("테스트 제목")
			.type(BoardPostType.QUESTION)
			.content("테스트 질문")
			.status(BoardPostStatus.WAITING)
			.build();
		ReflectionTestUtils.setField(post, "id", 10L);
		ReflectionTestUtils.setField(post, "createdAt", Instant.parse("2026-07-11T00:00:00Z"));
		ReflectionTestUtils.setField(post, "updatedAt", Instant.parse("2026-07-11T00:00:00Z"));
		return post;
	}
}
