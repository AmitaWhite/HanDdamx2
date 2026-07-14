package com.white.handdam.board.controller;

import com.white.handdam.board.dto.request.CreateBoardPostRequest;
import com.white.handdam.board.dto.response.BoardPostResponse;
import com.white.handdam.board.entity.BoardPostStatus;
import com.white.handdam.board.entity.BoardPostType;
import com.white.handdam.board.service.BoardPostService;
import com.white.handdam.global.response.ApiResponse;
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
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
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
	 * 권한: 게시판 크리에이터 본인 또는 활성 유료 구독자
	 */
	@GetMapping
	public ApiResponse<Page<BoardPostResponse>> getPosts(
		@PathVariable Long creatorId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestParam(required = false) BoardPostType type,
		@RequestParam(required = false) BoardPostStatus status,
		@PageableDefault(size = 5, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
	) {
		return ApiResponse.success(
			boardPostService.getPostsByCreator(creatorId, memberId, type, status, pageable)
		);
	}

	/**
	 * 유료 게시판 게시글·이미지 작성.
	 * images는 S3(LocalStack)에 업로드 후 url/storageKey 등으로 저장한다.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<BoardPostResponse>> createPost(
		@PathVariable Long creatorId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestParam @NotBlank @Size(max = 255) String title,
		@RequestParam @NotNull BoardPostType type,
		@RequestParam @NotBlank String content,
		@RequestPart(value = "images", required = false) List<MultipartFile> images
	) {
		BoardPostResponse response = boardPostService.createPost(
			creatorId,
			memberId,
			new CreateBoardPostRequest(title, type, content),
			images
		);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}
}
