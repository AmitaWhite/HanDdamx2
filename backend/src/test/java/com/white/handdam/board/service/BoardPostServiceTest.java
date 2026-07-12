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
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPost;
import com.white.handdam.board.entity.BoardPostImage;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.repository.BoardPostImageRepository;
import com.white.handdam.board.repository.BoardPostRepository;
import com.white.handdam.storage.ObjectStorage;
import com.white.handdam.storage.StoredObject;
import java.util.List;
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
import org.springframework.web.server.ResponseStatusException;

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
	@DisplayName("권한이 없으면 403에 해당하는 예외가 발생한다")
	void deniedWithoutPermission() {
		Long creatorId = 1L;
		Long strangerId = 7L;
		given(paidSubscriptionChecker.hasActivePaidSubscription(strangerId, creatorId)).willReturn(false);

		assertThatThrownBy(() ->
			boardPostService.getPostsByCreator(creatorId, strangerId, null, null, pageable)
		).isInstanceOf(ResponseStatusException.class);

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
			ReflectionTestUtils.setField(post, "createdAt", java.time.Instant.parse("2026-07-12T00:00:00Z"));
			ReflectionTestUtils.setField(post, "updatedAt", java.time.Instant.parse("2026-07-12T00:00:00Z"));
			return post;
		});
		given(boardPostImageRepository.saveAll(anyList())).willAnswer(invocation -> {
			List<BoardPostImage> images = invocation.getArgument(0);
			ReflectionTestUtils.setField(images.getFirst(), "id", 100L);
			ReflectionTestUtils.setField(images.getFirst(), "createdAt", java.time.Instant.parse("2026-07-12T00:00:00Z"));
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
			.isInstanceOf(ResponseStatusException.class);

		verify(boardPostRepository, never()).save(any());
		verifyNoInteractions(objectStorage);
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
		ReflectionTestUtils.setField(post, "createdAt", java.time.Instant.parse("2026-07-11T00:00:00Z"));
		ReflectionTestUtils.setField(post, "updatedAt", java.time.Instant.parse("2026-07-11T00:00:00Z"));
		return post;
	}
}
