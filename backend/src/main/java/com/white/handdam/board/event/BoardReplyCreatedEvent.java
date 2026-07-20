package com.white.handdam.board.event;

/**
 * 게시판 댓글에 대댓글이 작성되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code BoardNotificationListener} 가 구독해
 * <b>부모 댓글 작성자</b>에게 {@code BOARD_REPLY} 알림을 만든다.
 * (게시글 작성자가 아니다)
 *
 * <h3>발행 위치</h3>
 * {@code board/service/BoardCommentService#createReply} — {@code @Transactional} 안,
 * {@code saved} 지역 변수 생성 직후 (현재 118번 줄 부근).
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * BoardComment saved = boardCommentRepository.save(reply);
 * eventPublisher.publishEvent(new BoardReplyCreatedEvent(
 *     post.getId(),           // parent.getBoardPost() 로 이미 확보된 post (103번 줄)
 *     commentId,              // 파라미터 (부모 댓글)
 *     saved.getId(),
 *     requesterId,            // 파라미터 (대댓글 작성자)
 *     parent.getMemberId(),   // 이미 로드된 parent (96번 줄)
 *     request.content()
 * ));
 * }</pre>
 *
 * @param postId          대댓글이 속한 게시글 (알림 클릭 시 이동 대상)
 * @param parentCommentId 부모 댓글 ID
 * @param replyId         저장된 대댓글 ID
 * @param actorId         대댓글 작성자
 * @param recipientId     알림 수신자 = 부모 댓글 작성자
 * @param contentPreview  알림 문구에 쓸 대댓글 본문
 */
public record BoardReplyCreatedEvent(
	Long postId,
	Long parentCommentId,
	Long replyId,
	Long actorId,
	Long recipientId,
	String contentPreview
) {
}
