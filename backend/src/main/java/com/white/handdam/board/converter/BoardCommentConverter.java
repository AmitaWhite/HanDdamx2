package com.white.handdam.board.converter;

import com.white.handdam.board.dto.response.BoardCommentResponse;
import com.white.handdam.board.entity.BoardComment;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 댓글 Entity → API 응답 DTO 변환.
 * DB는 부모/자식을 한 테이블에 flat(1차원)으로 저장하고,
 * 응답은 최상위 댓글 아래에 replies를 중첩한 트리로 내려준다.
 */
public final class BoardCommentConverter {

	private static final String UNKNOWN = "알 수 없음";

	private BoardCommentConverter() {
	}

	public static List<BoardCommentResponse> toTree(List<BoardComment> comments) {
		return toTree(comments, Map.of());
	}

	public static List<BoardCommentResponse> toTree(
		List<BoardComment> comments,
		Map<Long, String> nicknameMap
	) {
		Map<Long, BoardCommentResponse> roots = new LinkedHashMap<>();
		Map<Long, List<BoardCommentResponse>> repliesByParent = new LinkedHashMap<>();

		for (BoardComment comment : comments) {
			if (comment.getDepth() == 0) {
				roots.put(
					comment.getId(),
					toResponse(comment, new ArrayList<>(), nicknameMap)
				);
			} else if (comment.getParentComment() != null) {
				Long parentId = comment.getParentComment().getId();
				repliesByParent
					.computeIfAbsent(parentId, ignored -> new ArrayList<>())
					.add(toResponse(comment, List.of(), nicknameMap));
			}
		}

		List<BoardCommentResponse> result = new ArrayList<>();
		for (BoardCommentResponse root : roots.values()) {
			List<BoardCommentResponse> replies =
				repliesByParent.getOrDefault(root.id(), List.of());
			result.add(copyWithReplies(root, replies));
		}
		return result;
	}

	public static BoardCommentResponse toResponse(
		BoardComment comment,
		List<BoardCommentResponse> replies
	) {
		return toResponse(comment, replies, Map.of());
	}

	public static BoardCommentResponse toResponse(
		BoardComment comment,
		List<BoardCommentResponse> replies,
		Map<Long, String> nicknameMap
	) {
		return new BoardCommentResponse(
			comment.getId(),
			comment.getBoardPost().getId(),
			comment.getMemberId(),
			nicknameMap.getOrDefault(comment.getMemberId(), UNKNOWN),
			comment.getParentComment() == null ? null : comment.getParentComment().getId(),
			comment.getDepth(),
			comment.getContent(),
			comment.isDeleted(),
			comment.getCreatedAt(),
			comment.getUpdatedAt(),
			comment.getDeletedAt(),
			replies
		);
	}

	private static BoardCommentResponse copyWithReplies(
		BoardCommentResponse root,
		List<BoardCommentResponse> replies
	) {
		return new BoardCommentResponse(
			root.id(),
			root.boardPostId(),
			root.memberId(),
			root.memberNickname(),
			root.parentCommentId(),
			root.depth(),
			root.content(),
			root.deleted(),
			root.createdAt(),
			root.updatedAt(),
			root.deletedAt(),
			replies
		);
	}
}
