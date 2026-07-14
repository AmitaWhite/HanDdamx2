package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostImageResponse;
import com.white.handdam.board.service.BoardPostService;
import com.white.handdam.global.response.ApiResponse;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/premium-board/posts/{postId}/images")
@RequiredArgsConstructor
public class BoardPostImageController {

	private final BoardPostService boardPostService;

	/**
	 * 기존 유료 게시글에 이미지 추가.
	 * 권한: 작성자, 공식 답변 전(WAITING)만.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ResponseEntity<ApiResponse<List<BoardPostImageResponse>>> addImages(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestPart("images") List<MultipartFile> images
	) {
		List<BoardPostImageResponse> response = boardPostService.addImages(postId, memberId, images);
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
	}

	/**
	 * 게시글 이미지 삭제.
	 * 권한: 작성자, 공식 답변 전(WAITING)만.
	 */
	@DeleteMapping("/{imageId}")
	public ResponseEntity<ApiResponse<Void>> deleteImage(
		@PathVariable Long postId,
		@PathVariable Long imageId,
		@RequestHeader("X-Member-Id") Long memberId
	) {
		boardPostService.deleteImage(postId, imageId, memberId);
		return ResponseEntity.status(HttpStatus.NO_CONTENT).body(ApiResponse.noContent());
	}
}
