package com.white.handdam.comment.event;

/**
 * 피드에 댓글이 작성되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code CommentNotificationListener} 가 구독해
 * 피드 작성자(크리에이터)에게 {@code FEED_COMMENT} 알림을 만든다.
 *
 * <h3>발행 위치</h3>
 * {@code comment/service/FeedCommentService#createComment} — {@code @Transactional} 안,
 * 댓글 저장 직후 (현재 114번 줄 부근).
 *
 * <p>현재 코드는 {@code return feedCommentRepository.save(comment).getId();} 로
 * save 가 return 문에 인라인되어 있으므로 <b>지역 변수 추출이 먼저 필요하다.</b>
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * FeedComment saved = feedCommentRepository.save(comment);
 * eventPublisher.publishEvent(new FeedCommentCreatedEvent(
 *     feedId,                    // 파라미터
 *     saved.getId(),
 *     memberId,                  // 파라미터 (댓글 작성자)
 *     project.getCreatorId(),    // 이미 로드된 project (104번 줄)
 *     request.content()
 * ));
 * return saved.getId();
 * }</pre>
 *
 * <p>{@code ApplicationEventPublisher} 는 {@code @RequiredArgsConstructor} 에 맞춰
 * {@code private final} 필드로 추가하면 된다 ({@code ChatMessageService} 와 동일).
 *
 * @param feedId        댓글이 달린 피드 (알림 클릭 시 이동 대상)
 * @param commentId     저장된 댓글 ID
 * @param actorId       댓글 작성자
 * @param recipientId   알림 수신자 = 피드 작성자(크리에이터).
 *                      작성자가 자기 피드에 댓글을 달면 알림은 자동으로 억제된다.
 * @param contentPreview 알림 문구에 쓸 댓글 본문 (길이 제한은 알림 쪽에서 처리)
 */
public record FeedCommentCreatedEvent(
	Long feedId,
	Long commentId,
	Long actorId,
	Long recipientId,
	String contentPreview
) {
}
