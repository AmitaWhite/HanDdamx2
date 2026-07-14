package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardPostRequest;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.service.BoardPostService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/creators/{creatorId}/premium-board/posts")
@RequiredArgsConstructor
@Validated
public class PremiumBoardPostController {

	private final BoardPostService boardPostService;

	/**
	 * 크리에이터별 유료 게시판 게시글 목록 조회.
	 *
	 * <pre>
	 * GET /api/v1/creators/{creatorId}/premium-board/posts
	 * 권한: 게시판 크리에이터 본인 또는 활성 유료 구독자
	 * </pre>
	 *
	 * 인증 인프라 연동 전: X-Member-Id 헤더로 호출자 식별 (이후 JWT로 교체).
	 */
	@GetMapping
	public Page<BoardPostResponse> getPosts(
		// URL 경로 — 어떤 크리에이터의 유료 게시판인지
		@PathVariable Long creatorId,
		// 요청자 ID (JWT 연동 전 임시 헤더)
		@RequestHeader("X-Member-Id") Long memberId,
		// 선택 필터: 게시글 유형 (QUESTION, GENERAL 등). 없으면 전체
		@RequestParam(required = false) BoardPostType type,
		// 선택 필터: 답변 상태 (WAITING / ANSWERED). 없으면 전체
		@RequestParam(required = false) BoardPostStatus status,
		// 페이지네이션: 기본 size=5, createdAt DESC
		@PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		// 권한 검사 + 삭제되지 않은 글 페이지 조회는 서비스에서 처리
		return boardPostService.getPostsByCreator(creatorId, memberId, type, status, pageable);
	}

	/**
	 * 유료 게시판 게시글·이미지 작성.
	 * images는 S3(LocalStack)에 업로드 후 url/storageKey 등으로 저장한다.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public BoardPostResponse createPost(
		@PathVariable Long creatorId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestParam @NotBlank @Size(max = 255) String title,
		@RequestParam @NotNull BoardPostType type,
		@RequestParam @NotBlank String content,
		@RequestPart(value = "images", required = false) List<MultipartFile> images
	) {
		return boardPostService.createPost(
			creatorId,
			memberId,
			new CreateBoardPostRequest(title, type, content),
			images
		);
	}
}
