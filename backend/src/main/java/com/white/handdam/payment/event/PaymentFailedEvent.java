package com.white.handdam.payment.event;

/**
 * 결제가 실패로 확정되었을 때 발행하는 도메인 이벤트.
 * 네이밍 규칙: {Aggregate}{PastTenseVerb}Event
 *
 * <p>{@code notification} 패키지의 {@code PaymentNotificationListener} 가 구독해
 * 결제를 시도한 본인에게 {@code PAYMENT_FAILED} 알림을 만든다.
 *
 * <h3>발행 위치 — 커밋되는 실패 경로 2곳</h3>
 * 둘 다 {@code payment/service/PaymentTransactionService} 안이다.
 * <ul>
 *   <li>{@code failConfirming} — Toss 승인 거절. 상태 변경 직후(현재 128번 줄 부근),
 *       {@code if (payment.isConfirming())} 블록 <b>안</b>.
 *       이 트랜잭션은 정상 커밋되고 예외는 그 뒤 호출자에서 던져지므로 AFTER_COMMIT 이 정상 동작한다.</li>
 *   <li>{@code fail} — 클라이언트가 실패를 보고한 경우. 현재 140번 줄 부근,
 *       {@code if (payment.isPending())} 블록 <b>안</b>.</li>
 * </ul>
 *
 * <p><b>주의:</b> {@code fail} 의 {@code if (payment.isFailed())} 조기 반환 분기(144번 줄 부근)에는
 * 발행하면 안 된다. 멱등 재시도 경로라 <b>알림이 중복 발송된다.</b>
 *
 * <p>{@code PaymentService#confirm} 의 타임아웃·API 예외 분기는 아무것도 저장하지 않으므로
 * (트랜잭션 자체가 없다) 알림을 만들 수 없다. 의도적으로 조용한 경로다.
 *
 * <h3>발행 예시</h3>
 * <pre>{@code
 * eventPublisher.publishEvent(new PaymentFailedEvent(
 *     payment.getId(),
 *     payment.getMemberId(),   // 결제 시도자 = 알림 수신자
 *     orderId,
 *     failureMessage
 * ));
 * }</pre>
 *
 * <h3>시스템 알림 — senderId 는 null</h3>
 * {@link PaymentSucceededEvent} 와 같다. 수신자를 {@code senderId} 에 넣으면
 * 자기알림 억제에 걸려 알림이 조용히 사라진다.
 *
 * @param paymentId      결제 ID (알림 클릭 시 이동 대상)
 * @param memberId       결제 시도자 = 알림 수신자
 * @param orderId        주문 번호 (로그 추적용)
 * @param failureMessage 실패 사유 (알림 문구용)
 */
public record PaymentFailedEvent(
	Long paymentId,
	Long memberId,
	String orderId,
	String failureMessage
) {
}
