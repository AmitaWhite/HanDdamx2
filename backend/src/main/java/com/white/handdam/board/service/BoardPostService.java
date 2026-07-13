package com.white.handdam.board.service;

import com.white.handdam.board.converter.BoardPostConverter;
import com.white.handdam.board.dto.request.CreateBoardPostRequest;
import com.white.handdam.board.dto.request.UpdateBoardPostRequest;
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

	// -------------------------------------------------------------------------
	// 목록 · 작성 (게시판 단위) — assertCanAccessBoard 공통
	// -------------------------------------------------------------------------

	/**
	 * 크리에이터별 유료 게시판 게시글 목록 조회.
	 *
	 * <pre>
	 * 1. assertCanAccessBoard — 크리에이터 본인 또는 활성 유료 구독자 (아니면 403)
	 * 2. findByCreator      — 삭제되지 않은 글만, type/status 선택 필터 + 페이지네이션
	 * 3. toResponse         — 엔티티 → BoardPostResponse (목록은 이미지 미포함)
	 * </pre>
	 *
	 * @param creatorId   게시판 소유 크리에이터 ID
	 * @param requesterId 요청자 ID
	 * @param type        선택 필터 (없으면 null → 전체 유형)
	 * @param status      선택 필터 (없으면 null → 전체 상태)
	 * @param pageable    페이지·정렬 정보
	 */
	public Page<BoardPostResponse> getPostsByCreator(
		Long creatorId,
		Long requesterId,
		BoardPostType type,
		BoardPostStatus status,
		Pageable pageable
	) {
		// 1) 게시판 접근 권한: 크리에이터 또는 활성 유료 구독자
		assertCanAccessBoard(creatorId, requesterId);
		// 2) 조건에 맞는 글 페이지 조회 후 DTO로 변환
		return boardPostRepository
			.findByCreator(creatorId, type, status, pageable)
			.map(BoardPostConverter::toResponse);
	}

	// -------------------------------------------------------------------------
	// 마이페이지 — 내가 작성한 글
	// -------------------------------------------------------------------------

	/**
	 * 내가 작성한 유료 게시판 글 목록 조회.
	 *
	 * <pre>
	 * 1. 로그인 확인 (memberId null → 403)
	 * 2. findByMember — member_id = 나, 삭제글 제외, type/status 필터 + 페이지네이션
	 * 3. toResponse
	 * </pre>
	 */
	public Page<BoardPostResponse> getMyPosts(
		Long memberId,
		BoardPostType type,
		BoardPostStatus status,
		Pageable pageable
	) {
		if (memberId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인이 필요합니다.");
		}
		return boardPostRepository
			.findByMember(memberId, type, status, pageable)
			.map(BoardPostConverter::toResponse);
	}

	/**
	 * 유료 게시판 게시글 작성.
	 *
	 * <pre>
	 * 흐름:
	 * 1. assertCanAccessBoard — 크리에이터 본인 또는 활성 유료 구독자만 허용 (아니면 403)
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
		assertCanAccessBoard(creatorId, requesterId);

		BoardPost post = BoardPost.builder()
			.creatorId(creatorId)
			.memberId(requesterId)
			.title(request.title())
			.type(request.type())
			.content(request.content())
			.status(BoardPostStatus.WAITING)
			.build();

		BoardPost saved = boardPostRepository.save(post);
		List<BoardPostImage> images = uploadAndSaveImages(saved, creatorId, imageFiles);
		return BoardPostConverter.toResponse(saved, images);
	}

	/**
	 * 게시글 이미지 처리. (`createPost`에서 호출)
	 *
	 * <pre>
	 * 각 파일마다:
	 * 1. objectStorage.upload  — S3(LocalStack)에 파일 저장, url/storageKey 반환
	 * 2. BoardPostImage 생성   — ERD 컬럼에 맞게 메타데이터 세팅
	 * 3. saveAll               — board_post_image 테이블에 일괄 INSERT
	 * </pre>
	 *
	 * order_index 는 업로드 순서(0, 1, 2...)로 부여한다.
	 * created_at 은 JPA Auditing(`BaseCreatedAtEntity`)에서 자동 설정된다.
	 *
	 * @param post        이미 저장된 board_post (id 필요 — 이미지 FK)
	 * @param creatorId   S3 폴더 경로에 사용 (premium-board/{creatorId}/...)
	 * @param imageFiles  multipart 이미지 목록 (null/빈 목록이면 이미지 없이 종료)
	 * @return            DB에 저장된 BoardPostImage 목록 (없으면 빈 리스트)
	 */
	private List<BoardPostImage> uploadAndSaveImages(
		BoardPost post,
		Long creatorId,
		List<MultipartFile> imageFiles
	) {
		// 첨부 없음 → 이미지 없이 글만 작성된 경우. DB/S3 작업 생략
		if (imageFiles == null || imageFiles.isEmpty()) {
			return List.of();
		}

		// S3 객체 키 prefix. 예: premium-board/3/uuid_a.jpg
		String folder = "premium-board/" + creatorId;
		// 업로드 성공한 이미지 엔티티를 모아둘 리스트
		List<BoardPostImage> images = new ArrayList<>();
		// 화면 노출 순서 시작값 (0부터 증가)
		int orderIndex = 0;

		for (MultipartFile file : imageFiles) {
			// multipart 빈 part(파일 미선택 등)는 건너뜀
			if (file == null || file.isEmpty()) {
				continue;
			}

			// S3 업로드 → storageKey(객체 키), url(접근 URL), originalName 반환
			StoredObject stored = objectStorage.upload(folder, file);

			// ERD BOARD_POST_IMAGE 컬럼 매핑
			BoardPostImage image = BoardPostImage.builder()
				.boardPost(post)                     // FK → board_post.id
				.url(stored.url())                   // 클라이언트 접근용 URL
				.storageKey(stored.storageKey())     // S3 객체 키 (NOT NULL)
				.originalName(stored.originalName()) // 원본 파일명
				.fileSize(file.getSize())            // byte 단위 크기
				.mimeType(file.getContentType())     // 예: image/jpeg
				.orderIndex(orderIndex++)            // 노출 순서 (업로드 순)
				.build();
			images.add(image);
		}

		// 유효 파일이 하나도 없었으면 INSERT 생략
		if (images.isEmpty()) {
			return List.of();
		}

		// board_post_image 일괄 저장 후, id/created_at 이 채워진 리스트 반환
		return boardPostImageRepository.saveAll(images);
	}

	/**
	 * 게시판(크리에이터) 단위 접근: 크리에이터 본인 또는 활성 유료 구독자.
	 * 목록·작성에서 공통 사용.
	 */
	void assertCanAccessBoard(Long creatorId, Long requesterId) {
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

	// -------------------------------------------------------------------------
	// 상세 조회 — assertCanAccessPost
	// -------------------------------------------------------------------------

	/**
	 * 유료 게시글 상세 조회.
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 권한 확인 — 게시판 크리에이터 / 글 작성자 / 활성 유료 구독자
	 * 3. 이미지 목록 조회 (order_index 오름차순)
	 * 4. BoardPostResponse 반환
	 * </pre>
	 */
	public BoardPostResponse getPost(Long postId, Long requesterId) {
		// 1) 소프트 삭제되지 않은 글만 조회. 없거나 삭제됨 → 404
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

		// 2) 게시판 크리에이터 / 작성자 / 활성 유료 구독자
		assertCanAccessPost(post, requesterId);

		// 3) 이미지 order_index 오름차순 (없으면 빈 리스트)
		List<BoardPostImage> images =
			boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(postId);
		return BoardPostConverter.toResponse(post, images);
	}

	/**
	 * 게시글 상세 접근: 게시판 크리에이터 / 작성자 / 활성 유료 구독자.
	 */
	void assertCanAccessPost(BoardPost post, Long requesterId) {
		if (requesterId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인이 필요합니다.");
		}
		if (requesterId.equals(post.getCreatorId())) {
			return;
		}
		if (requesterId.equals(post.getMemberId())) {
			return;
		}
		if (paidSubscriptionChecker.hasActivePaidSubscription(requesterId, post.getCreatorId())) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "유료 게시판에 접근할 권한이 없습니다.");
	}

	// -------------------------------------------------------------------------
	// 수정 · 삭제 — assertCanEditPost (작성자)
	// -------------------------------------------------------------------------

	/**
	 * 유료 게시글 수정 (공식 답변 전만).
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 작성자 권한 확인 (아니면 403)
	 * 3. status == WAITING 확인 (아니면 409)
	 * 4. title / type / content 갱신
	 * 5. 이미지 포함 BoardPostResponse 반환
	 * </pre>
	 *
	 * @param postId      수정할 게시글 ID
	 * @param requesterId 요청자(작성자여야 함)
	 * @param request     새 제목·유형·본문
	 */
	@Transactional
	public BoardPostResponse updatePost(Long postId, Long requesterId, UpdateBoardPostRequest request) {
		// 1) 소프트 삭제되지 않은 글만 조회. 없거나 삭제됨 → 404
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

		// 2) 작성자(member_id)만 수정 가능. 타인·비로그인 → 403
		assertCanEditPost(post, requesterId);

		// 3) ERD DECISION-006: 공식 답변 전(WAITING)만 수정 허용. ANSWERED → 409
		if (post.getStatus() != BoardPostStatus.WAITING) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "공식 답변이 등록된 게시글은 수정할 수 없습니다.");
		}

		// 4) 엔티티 필드 갱신. updated_at 은 JPA Auditing(`@LastModifiedDate`)에서 자동 설정
		post.update(request.title(), request.type(), request.content());

		// 5) 기존 이미지는 그대로 두고, 글+이미지로 응답 DTO 구성
		List<BoardPostImage> images =
			boardPostImageRepository.findByBoardPostIdOrderByOrderIndexAsc(postId);
		return BoardPostConverter.toResponse(post, images);
	}

	/**
	 * 유료 게시글 소프트 삭제.
	 *
	 * <pre>
	 * 1. 삭제되지 않은 게시글 조회 (없으면 404)
	 * 2. 작성자 권한 확인 (아니면 403)
	 * 3. is_deleted=true, deleted_at 설정
	 * </pre>
	 */
	@Transactional
	public void deletePost(Long postId, Long requesterId) {
		BoardPost post = boardPostRepository.findByIdAndDeletedFalse(postId)
			.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "게시글을 찾을 수 없습니다."));

		// 2) 작성자만 삭제 가능
		assertCanEditPost(post, requesterId);

		// 3) 소프트 삭제 (행은 유지, 목록·상세에서 제외)
		post.softDelete();
	}

	/**
	 * 게시글 수정·삭제: 작성자만 허용.
	 * (작성자는 유료 구독자이거나 게시판 소유 크리에이터인 경우만 글을 쓸 수 있음)
	 */
	void assertCanEditPost(BoardPost post, Long requesterId) {
		if (requesterId == null) {
			throw new ResponseStatusException(HttpStatus.FORBIDDEN, "로그인이 필요합니다.");
		}
		if (requesterId.equals(post.getMemberId())) {
			return;
		}
		throw new ResponseStatusException(HttpStatus.FORBIDDEN, "게시글을 수정할 권한이 없습니다.");
	}
}
