package com.white.handdam.board.event;

/**
 * 크리에이터가 유료 게시글에 공식 답변을 등록했을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code BoardNotificationListener} 가 구독해
 * <b>질문한 사람(게시글 작성자)</b>에게 {@code BOARD_ANSWER} 알림을 만든다.
 *
 * <h3>발행 위치 — 분기 안이 아니라 합류 지점</h3>
 * {@code board/service/BoardAnswerService#createAnswer} — {@code @Transactional} 안,
 * {@code post.markAnswered()} 직후 (현재 76번 줄 부근).
 *
 * <p><b>주의:</b> 이 메서드에는 답변 경로가 둘이다 —
 * 삭제된 답변을 되살리는 {@code restore} 분기(61번 줄 부근, {@code save()} 호출 없이 더티 체킹)와
 * 신규 {@code save} 분기(65번 줄 부근). <b>분기 안에서 발행하면 restore 경로가 조용히 누락된다.</b>
 * 두 분기가 합류한 뒤 {@code return} 직전에 한 번만 발행할 것.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * post.markAnswered();
 * eventPublisher.publishEvent(new BoardAnswerCreatedEvent(
 *     postId,                // 파라미터
 *     saved.getId(),         // 두 분기 모두 saved 에 담겨 있다
 *     requesterId,           // 파라미터 (답변한 크리에이터)
 *     post.getMemberId(),    // 이미 로드된 post (45번 줄) — 질문자
 *     request.content()
 * ));
 * }</pre>
 *
 * @param postId         답변이 달린 게시글 (알림 클릭 시 이동 대상)
 * @param answerId       저장된 답변 ID
 * @param actorId        답변을 작성한 크리에이터
 * @param recipientId    알림 수신자 = 게시글 작성자({@code post.getMemberId()})
 * @param contentPreview 알림 문구에 쓸 답변 본문
 */
public record BoardAnswerCreatedEvent(
	Long postId,
	Long answerId,
	Long actorId,
	Long recipientId,
	String contentPreview
) {
}
