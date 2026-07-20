package com.white.handdam.board.event;

/**
 * 유료 게시글에 댓글이 작성되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code BoardNotificationListener} 가 구독해
 * <b>게시글 작성자</b>에게 {@code BOARD_COMMENT} 알림을 만든다.
 *
 * <p>{@code BoardPost} 에는 비슷한 필드가 둘 있으니 주의 —
 * {@code creatorId} 는 게시판 주인(크리에이터), {@code memberId} 가 <b>글 작성자</b>다.
 * 수신자는 {@code memberId} 다.
 *
 * <h3>발행 위치</h3>
 * {@code board/service/BoardCommentService#createComment} — {@code @Transactional} 안,
 * {@code saved} 지역 변수 생성 직후 (현재 75번 줄 부근).
 * 이미 {@code saved} 가 있어 리팩터링 없이 한 줄만 추가하면 된다.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * BoardComment saved = boardCommentRepository.save(comment);
 * eventPublisher.publishEvent(new BoardCommentCreatedEvent(
 *     postId,                // 파라미터
 *     saved.getId(),
 *     requesterId,           // 파라미터 (댓글 작성자)
 *     post.getMemberId(),    // 이미 로드된 post (62번 줄) — 글 작성자
 *     request.content()
 * ));
 * }</pre>
 *
 * @param postId         댓글이 달린 게시글 (알림 클릭 시 이동 대상)
 * @param commentId      저장된 댓글 ID
 * @param actorId        댓글 작성자
 * @param recipientId    알림 수신자 = 게시글 작성자({@code post.getMemberId()})
 * @param contentPreview 알림 문구에 쓸 댓글 본문
 */
public record BoardCommentCreatedEvent(
	Long postId,
	Long commentId,
	Long actorId,
	Long recipientId,
	String contentPreview
) {
}
