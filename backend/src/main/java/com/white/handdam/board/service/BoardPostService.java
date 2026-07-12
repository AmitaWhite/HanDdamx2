package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardPostConverter;
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
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BoardPostService {

	private final BoardPostRepository boardPostRepository;
	private final BoardPostImageRepository boardPostImageRepository;
	private final PaidSubscriptionChecker paidSubscriptionChecker;
	private final ObjectStorage objectStorage;

	public Page<BoardPostResponse> getPostsByCreator(
		Long creatorId,
		Long requesterId,
		BoardPostType type,
		BoardPostStatus status,
		Pageable pageable
	) {
		assertCanAccess(creatorId, requesterId);
		return boardPostRepository
			.findByCreator(creatorId, type, status, pageable)
			.map(BoardPostConverter::toResponse);
	}

	/**
	 * 유료 게시판 게시글 작성.
	 *
	 * <pre>
	 * 흐름:
	 * 1. assertCanAccess  — 크리에이터 본인 또는 활성 유료 구독자만 허용 (아니면 403)
	 * 2. BoardPost 생성   — title/type/content 반영, status 기본값 WAITING
	 * 3. board_post 저장  — DB INSERT 후 id 발급 (이미지 FK에 필요)
	 * 4. 이미지 처리      — 파일이 있으면 S3 업로드 + board_post_image 저장
	 * 5. DTO 변환         — BoardPostResponse 로 반환
	 * </pre>
	 *
	 * @param creatorId   게시판 소유 크리에이터 ID
	 * @param requesterId 글 작성자(요청자) ID
	 * @param request     제목·유형·본문
	 * @param imageFiles  첨부 이미지(없으면 null/빈 리스트 가능)
	 */
	@Transactional
	public BoardPostResponse createPost(
		Long creatorId,
		Long requesterId,
		CreateBoardPostRequest request,
		List<MultipartFile> imageFiles
	) {
		// 1) 권한 검사: 통과하지 못하면 여기서 예외 → 아래 저장 로직 실행 안 됨
		assertCanAccess(creatorId, requesterId);

		// 2) 엔티티 조립 (아직 DB 반영 전, id 없음)
		//    creatorId = 게시판 주인, memberId = 실제 작성자
		BoardPost post = BoardPost.builder()
			.creatorId(creatorId)
			.memberId(requesterId)
			.title(request.title())
			.type(request.type())
			.content(request.content())
			.status(BoardPostStatus.WAITING) // 공식 답변 전: 답변 대기
			.build();

		// 3) board_post INSERT — 이 시점에 id / created_at / updated_at 이 채워짐
		BoardPost saved = boardPostRepository.save(post);

		// 4) 이미지 업로드·저장 (파일 없으면 빈 리스트)
		List<BoardPostImage> images = uploadAndSaveImages(saved, creatorId, imageFiles);

		// 5) 엔티티 → API 응답 DTO (글 + 이미지 목록)
		return BoardPostConverter.toResponse(saved, images);
	}

	/**
	 * 게시글 이미지 처리.
	 *
	 * <pre>
	 * 각 파일마다:
	 * 1. objectStorage.upload  — S3(LocalStack)에 파일 저장, url/storageKey 반환
	 * 2. BoardPostImage 생성   — ERD 컬럼에 맞게 메타데이터 세팅
	 * 3. saveAll               — board_post_image 테이블에 일괄 INSERT
	 * </pre>
	 *
	 * order_index 는 업로드 순서(0, 1, 2...)로 부여한다.
	 * created_at 은 엔티티 @PrePersist 에서 자동 설정된다.
	 */
	private List<BoardPostImage> uploadAndSaveImages(
		BoardPost post,
		Long creatorId,
		List<MultipartFile> imageFiles
	) {
		// 첨부 없음 → 이미지 없이 글만 작성된 경우
		if (imageFiles == null || imageFiles.isEmpty()) {
			return List.of();
		}

		// S3 키 prefix. 예: premium-board/3/uuid_a.jpg
		String folder = "premium-board/" + creatorId;
		List<BoardPostImage> images = new ArrayList<>();
		int orderIndex = 0;

		for (MultipartFile file : imageFiles) {
			// 비어 있는 part 는 건너뜀
			if (file == null || file.isEmpty()) {
				continue;
			}

			// S3 업로드 결과: storageKey, url, originalName
			StoredObject stored = objectStorage.upload(folder, file);

			// ERD BOARD_POST_IMAGE 컬럼 매핑
			BoardPostImage image = BoardPostImage.builder()
				.boardPost(post)                 // FK → board_post.id
				.url(stored.url())               // 접근 URL
				.storageKey(stored.storageKey()) // S3 객체 키 (NOT NULL)
				.originalName(stored.originalName())
				.fileSize(file.getSize())        // byte
				.mimeType(file.getContentType()) // 예: image/jpeg
				.orderIndex(orderIndex++)        // 노출 순서
				.build();
			images.add(image);
		}

		// 유효 파일이 하나도 없었으면 DB 저장 생략
		if (images.isEmpty()) {
			return List.of();
		}

		// board_post_image 일괄 저장 후, id/created_at 이 채워진 리스트 반환
		return boardPostImageRepository.saveAll(images);
	}

	/**
	 * ERD 정책: 해당 크리에이터의 활성 유료 구독자와 크리에이터만 접근 가능.
	 */
	void assertCanAccess(Long creatorId, Long requesterId) {
		if (requesterId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인이 필요합니다.");
		}
		if (requesterId.equals(creatorId)) {
			return;
		}
		if (paidSubscriptionChecker.hasActivePaidSubscription(requesterId, creatorId)) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "유료 게시판에 접근할 권한이 없습니다.");
	}
}
