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

	private BoardCommentConverter() {
	}

	/**
	 * flat 댓글 목록을 최상위 댓글 + replies 트리로 변환한다.
	 *
	 * <pre>
	 * 입력 예 (created_at 오름차순):
	 *   [부모A(depth0), 부모B(depth0), A의답글(depth1), A의답글2(depth1)]
	 *
	 * 출력 예:
	 *   [
	 *     부모A { replies: [A의답글, A의답글2] },
	 *     부모B { replies: [] }
	 *   ]
	 * </pre>
	 *
	 * @param comments created_at 오름차순으로 조회된 flat 목록이어야
	 *                 부모·대댓글 순서가 LinkedHashMap에 그대로 유지된다.
	 * LinkedHashMap : 넣은 순서를 기억한는 HashMap
	 */
	public static List<BoardCommentResponse> toTree(List<BoardComment> comments) {
		// 삽입 순서를 보존하기 위해 LinkedHashMap 사용 (HashMap이면 순서 깨짐)
		Map<Long, BoardCommentResponse> roots = new LinkedHashMap<>();
		// key = 부모 댓글 id, value = 그 부모에 달린 대댓글들
		Map<Long, List<BoardCommentResponse>> repliesByParent = new LinkedHashMap<>();

		// 1패스: 최상위(depth 0)와 대댓글(depth 1+)를 각각 다른 Map에 분류
		for (BoardComment comment : comments) {
			if (comment.getDepth() == 0) {
				// 최상위 댓글. replies는 아직 비워 두고 2패스에서 채운다
				roots.put(comment.getId(), toResponse(comment, new ArrayList<>()));
			} else if (comment.getParentComment() != null) {
				// 대댓글 → 부모 id 기준으로 묶음
				Long parentId = comment.getParentComment().getId();
				repliesByParent
					.computeIfAbsent(parentId, ignored -> new ArrayList<>())
					.add(toResponse(comment, List.of())); // 대댓글의 replies는 항상 빈 리스트
			}
			// parentComment가 null인 depth>0 은 비정상 데이터로 보고 무시
		}

		// 2패스: 각 최상위 댓글에 모아 둔 replies를 붙여 최종 응답 생성
		// record는 불변이라 replies만 바꿔 넣을 수 없어 새 BoardCommentResponse를 만든다
		List<BoardCommentResponse> result = new ArrayList<>();
		for (BoardCommentResponse root : roots.values()) {
			List<BoardCommentResponse> replies = repliesByParent.getOrDefault(root.id(), List.of());
			// 최상위 댓글에 모아 둔 replies를 붙여 최종 응답 생성, 새로운 BoardCommentResponse 객체 생성
			result.add(new BoardCommentResponse(
				root.id(),
				root.boardPostId(),
				root.memberId(),
				root.parentCommentId(),
				root.depth(),
				root.content(),
				root.deleted(),
				root.createdAt(),
				root.updatedAt(),
				root.deletedAt(),
				replies
			));
		}
		return result;
	}

	/**
	 * Entity 한 건을 Response로 매핑한다.
	 *
	 * @param comment Entity
	 * @param replies 하위에 붙일 대댓글 목록 (최상위는 toTree에서 채우고,
	 *                대댓글 자신은 보통 빈 리스트)
	 */
	public static BoardCommentResponse toResponse(BoardComment comment, List<BoardCommentResponse> replies) {
		return new BoardCommentResponse(
			comment.getId(),
			comment.getBoardPost().getId(),
			comment.getMemberId(),
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
}
