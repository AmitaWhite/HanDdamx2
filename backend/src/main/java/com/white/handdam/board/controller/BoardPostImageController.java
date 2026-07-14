package com.white.handdam.board.controller;

import com.white.handdam.board.dto.response.BoardPostImageResponse;
import com.white.handdam.board.service.BoardPostService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/premium-board/posts/{postId}/images")
@RequiredArgsConstructor
public class BoardPostImageController {

	private final BoardPostService boardPostService;

	/**
	 * 기존 유료 게시글에 이미지 추가.
	 * 권한: 작성자, 공식 답변 전(WAITING)만.
	 * images는 S3(LocalStack)에 업로드 후 board_post_image에 저장한다.
	 */
	@PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
	@ResponseStatus(HttpStatus.CREATED)
	public List<BoardPostImageResponse> addImages(
		@PathVariable Long postId,
		@RequestHeader("X-Member-Id") Long memberId,
		@RequestPart("images") List<MultipartFile> images
	) {
		return boardPostService.addImages(postId, memberId, images);
	}
}
