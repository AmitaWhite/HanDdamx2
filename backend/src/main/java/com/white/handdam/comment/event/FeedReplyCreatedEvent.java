package com.white.handdam.comment.event;

/**
 * 피드 댓글에 대댓글이 작성되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code CommentNotificationListener} 가 구독해
 * <b>부모 댓글 작성자</b>에게 {@code FEED_REPLY} 알림을 만든다.
 * (피드 작성자가 아니라 부모 댓글 작성자다 — 헷갈리기 쉬우니 주의)
 *
 * <h3>발행 위치</h3>
 * {@code comment/service/FeedCommentService#createReply} — {@code @Transactional} 안,
 * 대댓글 저장 직후 (현재 144번 줄 부근).
 *
 * <p>여기도 save 가 return 문에 인라인되어 있어 <b>지역 변수 추출이 먼저 필요하다.</b>
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * FeedComment saved = feedCommentRepository.save(reply);
 * eventPublisher.publishEvent(new FeedReplyCreatedEvent(
 *     feedId,                 // 파라미터
 *     parentCommentId,        // 파라미터
 *     saved.getId(),
 *     memberId,               // 파라미터 (대댓글 작성자)
 *     parent.getMemberId(),   // 이미 로드된 parent (132번 줄)
 *     request.content()
 * ));
 * return saved.getId();
 * }</pre>
 *
 * @param feedId          대댓글이 속한 피드 (알림 클릭 시 이동 대상)
 * @param parentCommentId 부모 댓글 ID
 * @param replyId         저장된 대댓글 ID
 * @param actorId         대댓글 작성자
 * @param recipientId     알림 수신자 = 부모 댓글 작성자.
 *                        자기 댓글에 자기가 답글을 달면 알림은 자동으로 억제된다.
 * @param contentPreview  알림 문구에 쓸 대댓글 본문
 */
public record FeedReplyCreatedEvent(
	Long feedId,
	Long parentCommentId,
	Long replyId,
	Long actorId,
	Long recipientId,
	String contentPreview
) {
}
