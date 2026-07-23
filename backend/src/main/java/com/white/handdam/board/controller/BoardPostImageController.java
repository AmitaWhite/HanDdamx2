package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostImageResponse;
import com.white.handdam.board.service.BoardPostService;
import com.white.handdam.global.response.ApiResponse;
import com.white.handdam.global.security.AuthMember;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/premium-board/posts/{postId}/images")
@RequiredArgsConstructor
public class BoardPostImageController {

	private final BoardPostService boardPostService;

	/**
	 * 기존 유료 게시글에 이미지 추가.
	 * 권한: 작성자, 공식 답변 전(WAITING)만. (JWT 인증 필요)
	 */
	@ResponseStatus(HttpStatus.CREATED)
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	public ApiResponse<List<BoardPostImageResponse>> addImages(
		@PathVariable Long postId,
		@AuthenticationPrincipal AuthMember member,
		@RequestParam("images") List<MultipartFile> images
	) {
		return ApiResponse.success(boardPostService.addImages(postId, member.id(), images));
	}

	/**
	 * 게시글 이미지 삭제.
	 * 권한: 작성자, 공식 답변 전(WAITING)만.
	 */
	@DeleteMapping("/{imageId}")
	public ApiResponse<Void> deleteImage(
		@PathVariable Long postId,
		@PathVariable Long imageId,
		@AuthenticationPrincipal AuthMember member
	) {
		boardPostService.deleteImage(postId, imageId, member.id());
		return ApiResponse.noContent();
	}
}
